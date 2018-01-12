/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4275")
public class GettersSettersOnRightFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    isGetterLike(methodTree.symbol()).ifPresent(fieldName -> checkFieldUsage(fieldName, methodTree, "getter"));
    isSetterLike(methodTree.symbol()).ifPresent(fieldName -> checkFieldUsage(fieldName, methodTree, "setter"));
  }

  private void checkFieldUsage(String fieldName, MethodTree methodTree, String accessor) {
    Symbol.TypeSymbol methodOwner = ((Symbol.TypeSymbol) methodTree.symbol().owner());
    if (methodOwner.lookupSymbols(fieldName).stream()
      .filter(Symbol::isVariableSymbol)
      .filter(s -> s.owner().isTypeSymbol())
      .noneMatch(s -> s.isPrivate() || s.isProtected())) {
      return;
    }
    FieldUsageVisitor fieldUsageVisitor = new FieldUsageVisitor(fieldName, methodOwner);
    methodTree.accept(fieldUsageVisitor);
    if (!fieldUsageVisitor.fieldIsUsed) {
      context.reportIssue(this, methodTree.simpleName(), "Refactor this " + accessor + " so that it actually refers to the field \"" + fieldName + "\".");
    }
  }

  private static Optional<String> isGetterLike(Symbol.MethodSymbol methodSymbol) {
    if (!methodSymbol.parameterTypes().isEmpty() || methodSymbol.isPrivate() || methodSymbol.isStatic()) {
      return Optional.empty();
    }
    String methodName = methodSymbol.name();
    if (methodName.length() > 3 && methodName.startsWith("get")) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(3)));
    }
    if (methodName.length() > 2 && methodName.startsWith("is")) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(2)));
    }
    return Optional.empty();
  }

  private static Optional<String> isSetterLike(Symbol.MethodSymbol methodSymbol) {
    if (methodSymbol.parameterTypes().size() != 1 || methodSymbol.isPrivate() || methodSymbol.isStatic()) {
      return Optional.empty();
    }
    String methodName = methodSymbol.name();
    if (methodName.length() > 3 && methodName.startsWith("set") && methodSymbol.returnType().type().isVoid()) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(3)));
    }
    return Optional.empty();
  }

  private static String lowerCaseFirstLetter(String methodName) {
    return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
  }

  private static class FieldUsageVisitor extends BaseTreeVisitor {

    private final String fieldName;
    private final Symbol fieldOwner;
    boolean fieldIsUsed;

    public FieldUsageVisitor(String fieldName, Symbol fieldOwner) {
      this.fieldName = fieldName;
      this.fieldOwner = fieldOwner;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol identifier = tree.symbol();
      fieldIsUsed = fieldIsUsed || (identifier.name().equals(fieldName) && identifier.owner() == fieldOwner);
    }
  }

}
