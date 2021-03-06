// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy.annotator.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import org.jetbrains.plugins.groovy.GroovyBundle.message
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.GrBlockLambdaBody
import org.jetbrains.plugins.groovy.lang.psi.api.GrLambdaExpression

class ConvertLambdaToClosureAction(lambda: GrLambdaExpression) : IntentionAction {

  private val myLambda: SmartPsiElementPointer<GrLambdaExpression> = lambda.createSmartPointer()

  override fun getText(): String = familyName

  override fun getFamilyName(): String = message("action.convert.lambda.to.closure")

  override fun startInWriteAction(): Boolean = true

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = myLambda.element != null

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val lambda = myLambda.element ?: return
    val closureText = closureText(lambda) ?: return
    val closure = GroovyPsiElementFactory.getInstance(project).createClosureFromText(closureText)
    lambda.replaceWithExpression(closure, false)
  }

  private fun closureText(lambda: GrLambdaExpression): String? {
    val parameterList = lambda.parameterList
    val body = lambda.body ?: return null

    val closureText = StringBuilder()
    closureText.append("{")
    if (parameterList.parametersCount != 0) {
      appendTextBetween(closureText, parameterList.text, parameterList.lParen, parameterList.rParen)
    }
    appendElements(closureText, parameterList, body)
    if (body is GrBlockLambdaBody) {
      appendTextBetween(closureText, body.text, body.lBrace, body.rBrace)
    }
    else {
      closureText.append(body.text)
    }
    closureText.append("}")
    return closureText.toString()
  }

  /**
   * Appends [text] to the [builder] cutting length of [left] text from the start and length of [right] text from the end.
   */
  private fun appendTextBetween(builder: StringBuilder, text: String, left: PsiElement?, right: PsiElement?) {
    val start = left?.textLength ?: 0
    val end = text.length - (right?.textLength ?: 0)
    builder.append(text, start, end)
  }

  /**
   * Appends text of elements to the [builder] between [start] and [stop].
   * If [stop] is `null` then all siblings of [start] are processed.
   */
  private fun appendElements(builder: StringBuilder, start: PsiElement, stop: PsiElement) {
    var current: PsiElement? = start.nextSibling
    while (current !== null && current !== stop) {
      builder.append(current.text)
      current = current.nextSibling
    }
  }
}
