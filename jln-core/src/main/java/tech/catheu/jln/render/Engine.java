/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln.render;


import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import io.methvin.watcher.DirectoryChangeEvent;
import net.openhft.compiler.CompilerUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.DefaultTokenWriter;
import spoon.support.compiler.VirtualFile;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.code.CtLocalVariableImpl;
import spoon.support.reflect.declaration.CtFieldImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import tech.catheu.jln.Main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Engine {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  // TODO CYRIL generate those randomly - at each run or only once + number of characters ?
  public static final String LINE_RETURN_REPLACEMENT = "_1PO";
  // should have high entropy to avoid collision
  public static final String SECRET_GENERATED_PRINT_PREFIX = "SECRET_";
  public static final AtomicInteger GENERATED_CLASS_ID = new AtomicInteger(0);

  // TODO MAKE THOSE fields at instantiation
  private static final Parser parser;
  private static final HtmlRenderer renderer;

  static {
    final MutableDataSet options = new MutableDataSet();
    final List<Extension> extensions = new ArrayList<>();
    extensions.add(TablesExtension.create());
    extensions.add(FootnoteExtension.create());
    extensions.add(GitLabExtension.create());
    options.set(Parser.EXTENSIONS, extensions);
    // convert soft-breaks to hard breaks
    options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
    options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
    options.set(HtmlRenderer.RENDER_HEADER_ID, true);
    parser = Parser.builder(options).build();
    renderer = HtmlRenderer.builder(options).build();
  }

  public static String renderingOf(@NonNull final DirectoryChangeEvent event) {
    final DirectoryChangeEvent.EventType type = event.eventType();
    final Path filePath = event.path();
    return switch (type) {
      case CREATE, MODIFY, OVERFLOW -> render(filePath);
      case DELETE ->
              "<div class=\"jln-error\"><pre>file " + filePath.toString() + "was deleted </pre></div>";
    };
  }

  public static String render(final Path filePath) {
    try {
      final String originalClassCode = Files.readString(filePath);
      final Result result = generateClass(originalClassCode);
      final String resOut =
              executeGeneratedClass(result.className(), result.customClass());
      return preparedUserStdOut(resOut);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
             InvocationTargetException | IOException e) {
      LOG.error("Failed rendering java class", e);
      return "<div class=\"jln-error\"><pre>" + e.getMessage() + "</pre></div>";
    }
  }

  @Nullable
  private static Result generateClass(String originalClassCode) {
    final Launcher spoon = new Launcher();
    spoon.getEnvironment().setAutoImports(true);
    spoon.addInputResource(new VirtualFile(originalClassCode));
    spoon.getFactory().getEnvironment().setPrettyPrinterCreator(() -> {
      DefaultJavaPrettyPrinter defaultJavaPrettyPrinter =
              new DefaultJavaPrettyPrinter(spoon.getFactory().getEnvironment());
      defaultJavaPrettyPrinter.setIgnoreImplicit(false);
      defaultJavaPrettyPrinter.setPrinterTokenWriter(new DefaultTokenWriter() {
        @Override
        public DefaultTokenWriter writeComment(CtComment comment) {
          // FIXME P1 CYRIL maybe just don't write comments for top level field impl, main because they are written manually as markdown
          //  is it possible to get the top level? most likely
          // don't write comments
          return this;
        }
      });
      return defaultJavaPrettyPrinter;
    });
    final CtModel model = spoon.buildModel();
    final CtClass<?> classAst = (CtClass<?>) model.getAllTypes().iterator().next();
    final List<CtTypeMember> members = classAst.getTypeMembers();
    final StringBuilder newMain = new StringBuilder();
    newMain.append("public static void main(String[] args) { \n");
    SourcePosition mainPosition = null;
    for (int i = 1; i < members.size(); i++) {
      final CtTypeMember elem = members.get(i);
      newMain.append(commentPrint(elem));
      if (elem instanceof CtFieldImpl field) {
        newMain.append(fieldPrint(field));
      } else if (elem instanceof CtMethodImpl<?> method) {
        if (method.getSimpleName()
                  .equals("main")) { // FIXME CYRIL make more robust check static and params 
          mainPosition = method.getPosition();
          mainBodyPrint(method, newMain);
        } else {
          newMain.append(methodPrint(method));
        }
      } else {
        throw new UnsupportedOperationException("Unsupported class element type: " + elem.getClass());
      }
    }
    newMain.append("}");
    Objects.requireNonNull(mainPosition, "main method not found in class");
    var n = classAst.getSimpleName();
    final String customClass = originalClassCode.substring(0,
                                                           mainPosition.getSourceStart()) + newMain + originalClassCode.substring(
            mainPosition.getSourceEnd() + 1);
    final String className = classAst.getQualifiedName();
    return new Result(customClass, className);
  }

  private record Result(String customClass, String className) {
  }

  /**
   * execute a generated class intercept stdout in a single string and return it
   * NOTE: most likely not the best way to do compile and run - anyway going for this for the moment
   * FIXME should run on a separate classloader
   */
  private static String executeGeneratedClass(final @NonNull String className,
                                              final @NonNull String customClass) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final ClassLoader cl = new URLClassLoader(new URL[0]);
    final Class aClass =
            CompilerUtils.CACHED_COMPILER.loadFromJava(cl, className, customClass);
    final Method meth = aClass.getMethod("main", String[].class);
    final String[] params = null; // init params accordingly
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream outPrintStream = new PrintStream(out);
    final PrintStream previousOutStream = System.out;
    final PrintStream previousErrStream = System.err;
    System.setOut(outPrintStream);
    System.setErr(outPrintStream);
    meth.invoke(null, (Object) params);
    System.setOut(previousOutStream);
    System.setErr(previousErrStream);
    final String resOut = out.toString();
    out.reset();
    return resOut;
  }

  // 

  /**
   * Find stdout lines that were not generated by this class (generated by the user), and format them in html.
   * Note: brittle. A lot of string manipulation hacks are done here.
   * The idea is to prepend a SECRET suffix to all generated out lines.
   * The lines that don't have this suffix are generated by user code we don't control.
   * Find these lines and render them.
   */
  @NotNull
  private static String preparedUserStdOut(String resOut) {
    final String[] lines = resOut.split(SECRET_GENERATED_PRINT_PREFIX);
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String l = lines[i];
      if (l.isEmpty()) {
        continue;
      }
      String[] s = l.split("\n", 2);
      result.append(s[0]).append("\n");
      if (s.length > 1 && !s[1].isEmpty()) {
        result.append(renderStdOut(s[1]));
      }
    }
    return result.toString().replaceAll(LINE_RETURN_REPLACEMENT, "\n");
  }

  private static void mainBodyPrint(CtMethodImpl<?> method, StringBuilder newMain) {
    CtBlockImpl<?> block = (CtBlockImpl<?>) method.getBody();
    List<CtStatement> statements = block.getStatements();
    final List<String> codeElems = new ArrayList<>();
    for (int i = 0; i < statements.size(); i++) {
      CtStatement s = statements.get(i);
      var sPos = s.getPosition();
      var endLine = sPos.getEndLine();
      newMain.append(commentPrint(s));
      if (s instanceof CtComment) {
        continue;
      }
      final String statement = s.toString() + ";";
      codeElems.add(statement);
      boolean showResult = i + 1 == statements.size() // end of file
                           || statements.get(i + 1) instanceof CtComment // next is a comment
                           || statements.get(i + 1)
                                        .getPosition()
                                        .getLine() > endLine + 1; // there is an empty line --> corresponds to a notebook block end
      if (showResult) {
        newMain.append(renderCodeStatement(String.join("\n", codeElems)));
        codeElems.forEach(newMain::append);
        codeElems.clear();
        if (s instanceof CtLocalVariableImpl<?> variable) {
          newMain.append(renderValueStatement(variable.getSimpleName()));
        }
      }
    }
  }

  private static String methodPrint(CtMethodImpl<?> method) {
    return renderCodeStatement(method.toString());
  }

  private static String commentPrint(CtElement elem) {
    // FIXME CYRIL P2 - test doc comment
    if (elem.getComments().isEmpty()) {
      return "";
    }
    final String commentsString = elem.getComments()
                                      .stream()
                                      .map(CtComment::getContent)
                                      .collect(Collectors.joining("\n"));
    final String commentMarkdown = markdownToHtml(commentsString);
    final String markdownEscaped =
            commentMarkdown.replaceAll("\n", LINE_RETURN_REPLACEMENT);
    return renderCommentStatement(markdownEscaped);
  }

  private static String fieldPrint(CtField field) {
    return renderCodeStatement(field.toString());
  }

  // FIXME P1 - escape html 
  private static String renderStdOut(final String val) {
    return "<pre class=\"stdout\">" + val + "</pre>";
  }

  // FIXME P0 - implement renderer based on type of objects
  // FIXME P1 - make it possible to inject new renderers 
  private static String renderValueStatement(final String varName) {
    return "System.out.println(\"" + SECRET_GENERATED_PRINT_PREFIX + "<pre class=\\\"result\\\">\" + " + varName + " + \"</pre>\");\n";
  }

  private static String renderCodeStatement(final String codeBlock) {
    return "System.out.println(\"" + SECRET_GENERATED_PRINT_PREFIX + "<pre class=\\\"code\\\"><code>" + codeBlock.replaceAll(
            "\n",
            LINE_RETURN_REPLACEMENT).replaceAll("\"", "\\\\\"") + "</code></pre>\");\n";
  }

  private static String renderCommentStatement(final String commentBlock) {
    return "System.out.println(\"" + SECRET_GENERATED_PRINT_PREFIX + "<section>" + commentBlock.replaceAll(
            "\n",
            LINE_RETURN_REPLACEMENT).replaceAll("\"", "\\\\\"") + "</section>\");\n";
  }

  private static String markdownToHtml(final String markdown) {
    final Node document = parser.parse(markdown);
    return renderer.render(document);
  }
}
