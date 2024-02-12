@import tech.catheu.jln.Main.InteractiveConfiguration
@param tech.catheu.jln.server.HtmlTemplateEngine.TemplateData model

<div id="helper">
    <div class="relative">
        <div class="overflow-y-hidden">
            <p>Welcome to <code>java-live-notebook</code> aka <code>jln</code>. Learn more on <a href="https://jnotebook.catheu.tech">jln.catheu.tech</a>.
            </p>
            <p>Edit a <code>.java</code> file in your
                <code>${((InteractiveConfiguration) model.config()).notebookPath}</code> folder to
                launch your notebook, or click on a notebook below.</p>
            <div>
                <table>
                    <tr>
                        <td>Notebooks</td>
                    </tr>
                    @if(model.notebooksInPath() == null || model.notebooksInPath().isEmpty())
                        <tr>
                            <td>
                                <i>There are no notebooks in the <code>${((InteractiveConfiguration) model.config()).notebookPath}</code> folder.</i>
                            </td>
                        </tr>
                    @else
                        @for(final java.nio.file.Path p: model.notebooksInPath())
                            <tr>
                                <td><button class="notebook-in-path"><a>${p.toString()}</a></button></td>
                            </tr>
                        @endfor
                    @endif
                </table>
            </div>
        </div>
    </div>
</div>
