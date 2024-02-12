@import tech.catheu.jln.Main.InteractiveConfiguration
@import tech.catheu.jln.server.HtmlTemplateEngine.TemplateData
@import java.util.Optional
@param TemplateData model
<!--/*

    Copyright 2023 Cyril de Catheu

    Use of this source code is governed by an MIT-style
    license that can be found in the LICENSE file or at
    https://opensource.org/licenses/MIT.

*/-->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>notebook</title>
    <link rel="icon"
          href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>📓</text></svg>">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"
            crossorigin="anonymous"></script>

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/default.min.css">
    <script defer class="jnb-opti" src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/highlight.min.js" ,
            onload="renderCodeColor();"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/tocbot/4.18.2/tocbot.css">
    <script>
        function tocCopy() {
            const sourceDiv = document.getElementById('toc');
            const destinationDiv = document.getElementById('tocCopy');
            destinationDiv.innerHTML = sourceDiv.innerHTML;
        }

        function initTocBot() {
            tocbot.init({
                // Where to render the table of contents.
                tocSelector: '#toc',
                // Where to grab the headings to build the table of contents.
                contentSelector: '#notebook',
                // Which headings to grab inside of the contentSelector element.
                headingSelector: 'h1, h2, h3',
                // For headings inside relative or absolute positioned containers within content.
                hasInnerContainers: true,
            });
            tocCopy();
        }
    </script>
    <script defer class="jnb-opti" src="https://cdnjs.cloudflare.com/ajax/libs/tocbot/4.18.2/tocbot.min.js" ,
            onload="initTocBot();"></script>

    <style>
        html {
            font-size: 18px;
        }

        @media (max-width: 600px) {
            html {
                font-size: 16px;
            }
        }

        body {
            font-family: ui-serif, Georgia, Cambria, "Times New Roman", Times, serif;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
            color: rgb(17 24 39);
            /* TODO CYRIL - sm:overscroll-y-none; */
        }

        /*TODO introduce a class for markdown rather than using the tag section*/
        section {
            padding-left: 0.25rem;
            margin-bottom: 0;
            margin-top: 1rem;
        }

        #notebook > :first-child {
            margin-top: 0;
        }

        .code {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
            font-size: 0.875rem; /* 14px */
            line-height: 1.25rem; /* 20px */
            color: rgb(17 24 39);
            background-color: rgb(241 245 249);
            margin-bottom: 0;
            padding-left: 0.25rem;
            margin-top: 1rem;
        }

        .result, .stdout {
            border-right: 2px solid rgb(241 245 249);
            border-bottom: 2px solid rgb(241 245 249);
            border-left: 2px solid rgb(241 245 249);
            padding-left: 0.5rem;
            margin-bottom: 0;
        }

        .status-header {
            font-family: "Fira Sans Condensed", sans-serif;
        }

        .toc-column {
            height: 100%;
            padding-left: 0.1rem;

            & ol {
                list-style: none;
            }

            & a {
                text-decoration: none;

            }
        }

        .toc-button {
            padding: 0;
            color: black;
        }

    </style>
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <div id="toc-column" class="toc-column d-md-block d-none col-2 container-fluid sticky-top">
            <nav id="toc"></nav>
        </div>
        <div class="offcanvas offcanvas-start" tabindex="-1" id="offcanvasToc"
             aria-labelledby="offcanvasTocLabel">
            <div class="offcanvas-header">
                <h5 class="offcanvas-title" id="offcanvasTocLabel">Content</h5>
                <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Close"></button>
            </div>
            <div class="toc-column offcanvas-body">
                <nav id="tocCopy"></nav>
            </div>
        </div>
        <div class="col">
            <div class="container-fluid sticky-top">
                <div class="row status-header bg-white align-items-center">
                    <a class="toc-button col-1 d-md-none" data-bs-toggle="offcanvas" href="#offcanvasToc"
                       role="button"
                       aria-controls="offcanvasToc">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                             width="30"
                             height="30">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M4 6h16M4 12h16M4 18h16"></path>
                        </svg>
                    </a>
                    @if(model.interactive())
                        <div class="col text-center text-success">
                            <span id="status-message" data-status="" class="status-info"></span>
                        </div>
                    @endif
                    <div class="col-2-md col-3 text-end text-secondary">
                        <p style="font-size: small; margin-bottom: 0">Built with <a class="text-secondary" href="https://github.com/cyrilou242/java-live-notebook" target="_blank">JLN</a></p>
                    </div>
                </div>
            </div>
            <div id="notebook" class="container">
                @if(model.render() != null)
                    $unsafe{model.render()}
                @else
                    @template.landing(model=model)
                @endif
            </div>
        </div>
    </div>
</div>
@if(model.interactive())
    <script defer>
        function renderCodeColor() {
            document.querySelectorAll('.code').forEach(el => {
                hljs.highlightElement(el, {language: 'java'});
            });
        }

        function reRender() {
            renderCodeColor()
            tocbot.refresh();
            tocCopy();
        }

        function update(eventData) {
            document.getElementById("notebook").innerHTML = eventData;
            reRender();
            updateStatus("CONNECTED");
        }

        function updateStatus(status) {
            console.log("Status change: " + status);
            const statusElem = document.getElementById("status-message");
            statusElem.textContent = status;
        }

        function startWebSocketServer(port) {
            var socket = new WebSocket("ws://localhost:" + port + "/websocket");

            socket.onclose = function (event) {
                console.log("Websocket connection closed or unable to connect; " +
                    "starting reconnect timeout");
                updateStatus("DISCONNECTED");
                socket = null;
                setTimeout(function () {
                    startWebSocketServer(port);
                }, 5000)
            }

            socket.onmessage = (socketEvent) => {
                if (socketEvent.data.startsWith("status_")) {
                    updateStatus(socketEvent.data.substring(7))
                } else {
                    update(socketEvent.data);
                }
            };

            // something like this for the message passing
            const textAreas = document.querySelectorAll("button.notebook-in-path")
                .forEach(e => e.addEventListener('click', ev => {
                    socket.send("refresh_" + e.textContent)
                }));
        }

        const port = "${Optional.ofNullable(((InteractiveConfiguration) model.config()).port).orElse(5002)}";
        startWebSocketServer(port);
    </script>
@endif
</body>
</html>
