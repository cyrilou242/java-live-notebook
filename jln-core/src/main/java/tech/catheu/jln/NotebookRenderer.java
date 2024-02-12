/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln;

import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jln.render.Engine;
import tech.catheu.jln.server.HtmlTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static tech.catheu.jln.utils.JavaUtils.optional;

public class NotebookRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(NotebookRenderer.class);

  public static NotebookRenderer from(final Main.SharedConfiguration config) {
    return new NotebookRenderer();
  }

  protected NotebookRenderer() {
  }

  public void render(final Main.RenderConfiguration config) {

    final Path filePath = Paths.get(config.inputPath);
    try {
      final String rendering = Engine.render(filePath);
      final HtmlTemplateEngine templateEngine = new HtmlTemplateEngine();
      final HtmlTemplateEngine.TemplateData model =
              new HtmlTemplateEngine.TemplateData(config, false, rendering, null);
      String html = templateEngine.render(model);
      if (!config.noOptimize) {
        html = optimizeHtml(html);
      }

      final String outputPath =
              optional(config.outputPath).orElse(Files.getNameWithoutExtension(config.inputPath) + ".html");
      final File outputFile = FileUtils.getFile(outputPath);
      FileUtils.write(outputFile, html, StandardCharsets.UTF_8);
      LOG.info("Notebook rendered successfully and written to {}", outputFile);
    } catch (Exception e) {
      throw new RuntimeException(String.format("Exception rendering notebook %s: ",
                                               filePath) + e);
    }
  }

  private String optimizeHtml(String html) {
    // remove scripts that cannot be optimized
    final Document originalDoc = Jsoup.parse(html);
    originalDoc.outputSettings().prettyPrint(false);
    final Elements noOptiScripts = originalDoc.head().select(".jnb-no-opti").remove();
    final String htmlForOpti = originalDoc.outerHtml();

    // serve file
    HtmlFileServer miniServer = getMiniServer(htmlForOpti);
    miniServer.server.start();
    // use selenium to render the file with javascript in a browser and parse the content
    ChromeOptions options = new ChromeOptions().addArguments("--headless=new");
    WebDriverManager.chromedriver().setup();
    final WebDriver webDriver = new ChromeDriver(options);
    webDriver.get(miniServer.url);
    final String htmlWithOpti = webDriver.getPageSource();
    webDriver.quit();
    miniServer.server().stop(0);

    // remove scripts that were optimized
    final Document notebookWithOpti = Jsoup.parse(htmlWithOpti);
    notebookWithOpti.outputSettings().prettyPrint(false);
    // remove the scripts that are not necessary anymore
    notebookWithOpti.head().select(".jnb-opti").remove();

    // put back scripts that cannot be optimized
    // TODO - filter scripts and stylesheet that are not used
    notebookWithOpti.head().appendChildren(noOptiScripts);

    return "<!DOCTYPE html>\n" + notebookWithOpti.outerHtml();
  }

  @NotNull
  private NotebookRenderer.HtmlFileServer getMiniServer(final String htmlFile) {
    final byte[] htmlBytes = htmlFile.getBytes(StandardCharsets.UTF_8);
    try {
      final int port = getFreePort();
      final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
          exchange.getResponseHeaders().set("Content-Type", "text/html");
          exchange.sendResponseHeaders(200, htmlBytes.length);
          OutputStream responseStream = exchange.getResponseBody();
          responseStream.write(htmlBytes);
          responseStream.close();
        }
      });
      final HtmlFileServer result =
              new HtmlFileServer("http://localhost:" + port, server);
      return result;
    } catch (Exception e) {
      LOG.error("Failed to create a server: ", e);
      throw new RuntimeException("Failed to create a server.", e);
    }
  }

  private int getFreePort() {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      checkNotNull(serverSocket,
                   "Failed to find a free port: failed creating a ServerSocket instance.");
      checkState(serverSocket.getLocalPort() > 0, "Failed to find a free port.");
      final int localPort = serverSocket.getLocalPort();
      serverSocket.close();
      return localPort;
    } catch (IOException e) {
      throw new RuntimeException("Failed to find a free port", e);
    }
  }

  private record HtmlFileServer(String url, HttpServer server) {
  }
}
