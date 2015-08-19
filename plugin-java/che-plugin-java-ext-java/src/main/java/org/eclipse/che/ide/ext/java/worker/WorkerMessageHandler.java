/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.worker;

import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.collections.js.JsoStringMap;
import org.eclipse.che.ide.ext.java.jdt.CUVariables;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.ContentAssistHistory;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.TemplateCompletionProposalComputer;
import org.eclipse.che.ide.ext.java.jdt.core.JavaCore;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTParser;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.formatter.CodeFormatter;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.impl.AssistOptions;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.che.ide.ext.java.jdt.templates.CodeTemplateContextType;
import org.eclipse.che.ide.ext.java.jdt.templates.ContextTypeRegistry;
import org.eclipse.che.ide.ext.java.jdt.templates.ElementTypeResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.ExceptionVariableNameResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.FieldResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.ImportsResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.JavaContextType;
import org.eclipse.che.ide.ext.java.jdt.templates.JavaDocContextType;
import org.eclipse.che.ide.ext.java.jdt.templates.LinkResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.LocalVarResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.NameResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.StaticImportResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.TemplateStore;
import org.eclipse.che.ide.ext.java.jdt.templates.TypeResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.TypeVariableResolver;
import org.eclipse.che.ide.ext.java.jdt.templates.VarResolver;
import org.eclipse.che.ide.ext.java.messages.ConfigMessage;
import org.eclipse.che.ide.ext.java.messages.DependenciesUpdatedMessage;
import org.eclipse.che.ide.ext.java.messages.FileClosedMessage;
import org.eclipse.che.ide.ext.java.messages.FormatMessage;
import org.eclipse.che.ide.ext.java.messages.ParseMessage;
import org.eclipse.che.ide.ext.java.messages.PreferenceFormatSetMessage;
import org.eclipse.che.ide.ext.java.messages.Problem;
import org.eclipse.che.ide.ext.java.messages.RemoveFqnMessage;
import org.eclipse.che.ide.ext.java.messages.RoutingTypes;
import org.eclipse.che.ide.ext.java.messages.impl.MessagesImpls;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopySourceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopyTargetEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopyingRangeMarker;
import org.eclipse.che.ide.ext.java.jdt.text.edits.DeleteEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.InsertEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MoveSourceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MoveTargetEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MultiTextEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.RangeMarker;
import org.eclipse.che.ide.ext.java.jdt.text.edits.ReplaceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.TextEdit;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.gwt.webworker.client.messages.MessageFilter;
import com.google.gwt.webworker.client.messages.MessageImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
public class WorkerMessageHandler implements MessageHandler, MessageFilter.MessageRecipient<ParseMessage> {

    private static WorkerMessageHandler        instance;
    private final  WorkerOutlineModelUpdater   outlineModelUpdater;
    private final  WorkerJavadocHandleComputer javadocHandleComputer;
    private final WorkerCuCache cuCache;
    private WorkerCorrectionProcessor correctionProcessor;
    private INameEnvironment          nameEnvironment;
    private HashMap<String, String> options                  = new HashMap<String, String>();
    private Map<String, String>     preferenceFormatSettings = new HashMap<String, String>();
    private MessageFilter                      messageFilter;
    private JavaParserWorker                   worker;
    private ContentAssistHistory               contentAssistHistory;
    private ContextTypeRegistry                fCodeTemplateContextTypeRegistry;
    private TemplateStore                      templateStore;
    private String                             projectName;
    private CUVariables                        cuVar;
    private TemplateCompletionProposalComputer templateCompletionProposalComputer;
    private WorkerCodeAssist                   workerCodeAssist;

    public WorkerMessageHandler(final JavaParserWorker worker) {
        this.worker = worker;
        instance = this;
        initOptions();
        messageFilter = new MessageFilter();
        cuCache = new WorkerCuCache();
        javadocHandleComputer = new WorkerJavadocHandleComputer(worker, cuCache);
        MessageFilter.MessageRecipient<ConfigMessage> configMessageRecipient = new MessageFilter.MessageRecipient<ConfigMessage>() {
            @Override
            public void onMessageReceived(ConfigMessage config) {
                nameEnvironment =
                        new WorkerNameEnvironment(config.caPath(), config.restContext(), config.wsId());
                projectName = config.projectName();
                WorkerProposalApplier applier = new WorkerProposalApplier(WorkerMessageHandler.this.worker, messageFilter);
                workerCodeAssist =
                        new WorkerCodeAssist(WorkerMessageHandler.this.worker, messageFilter, applier, nameEnvironment,
                                             templateCompletionProposalComputer,
                                             config.javaDocContext(), cuCache);
                correctionProcessor = new WorkerCorrectionProcessor(WorkerMessageHandler.this.worker, messageFilter, applier, cuCache);
            }
        };
        messageFilter.registerMessageRecipient(RoutingTypes.CONFIG, configMessageRecipient);
        messageFilter.registerMessageRecipient(RoutingTypes.PARSE, this);
        templateCompletionProposalComputer = new TemplateCompletionProposalComputer(getTemplateContextRegistry());
        outlineModelUpdater = new WorkerOutlineModelUpdater(worker);
        messageFilter.registerMessageRecipient(RoutingTypes.REMOVE_FQN, new MessageFilter.MessageRecipient<RemoveFqnMessage>() {
            @Override
            public void onMessageReceived(RemoveFqnMessage message) {
                WorkerTypeInfoStorage.get().removeFqn(message.fqn());
            }
        });

        messageFilter.registerMessageRecipient(RoutingTypes.FORMAT, new MessageFilter.MessageRecipient<FormatMessage>() {
            @Override
            public void onMessageReceived(FormatMessage message) {
                if (preferenceFormatSettings != null) {
                    TextEdit edit;
                    if (message.offset() == 0) {
                        edit =
                                CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, message.content(), 0, null,
                                                          preferenceFormatSettings);
                    } else {
                        edit =
                                CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, message.content(), message.offset(),
                                                          message.length(), 0, null, preferenceFormatSettings);
                    }
                    Jso textEditJso = convertTextEditToJso(edit);
                    MessagesImpls.FormatResultMessageImpl formatResultMes = MessagesImpls.FormatResultMessageImpl.make();
                    formatResultMes.setTextEdit(textEditJso);
                    formatResultMes.setId(message.id());
                    worker.sendMessage(formatResultMes.serialize());
                }
            }
        });
        messageFilter.registerMessageRecipient(RoutingTypes.PREFERENCE_FORMAT_SETTINGS,
                                               new MessageFilter.MessageRecipient<PreferenceFormatSetMessage>() {
                                                   @Override
                                                   public void onMessageReceived(PreferenceFormatSetMessage message) {
                                                       JsoStringMap<String> settingsJso = message.settings();
                                                       for (String key: settingsJso.getKeys().asIterable()) {
                                                           preferenceFormatSettings.put(key, settingsJso.get(key));
                                                       }
                                                   }
                                               });
        messageFilter.registerMessageRecipient(RoutingTypes.DEPENDENCIES_UPDATED, new MessageFilter.MessageRecipient<DependenciesUpdatedMessage>() {
            @Override
            public void onMessageReceived(DependenciesUpdatedMessage message) {
                if(nameEnvironment != null) {
                    nameEnvironment.clearBlackList();
                }
                WorkerTypeInfoStorage.get().clear();
            }
        });

        messageFilter.registerMessageRecipient(RoutingTypes.FILE_CLOSED, new MessageFilter.MessageRecipient<FileClosedMessage>() {
            @Override
            public void onMessageReceived(FileClosedMessage message) {
                cuCache.removeCompilationUnit(message.getFilePath());
            }
        });

        messageFilter.registerMessageRecipient(RoutingTypes.COMPUTE_JAVADOC_HANDE, javadocHandleComputer);

    }

    public static WorkerMessageHandler get() {
        return instance;
    }

    /** Creates a JsoArray from a Java array. */
    public static <M> JsoArray<M> from(M... array) {
        JsoArray<M> result = JsoArray.create();
        for (M s : array) {
            if(s != null)
              result.add(s);
        }
        return result;
    }

    private Jso convertTextEditToJso(TextEdit edit) {
        Jso textEdit = Jso.create();
        textEdit.addField("offSet", edit.getOffset());

        if (edit.hasChildren()) {
            textEdit.addField("children", edit.getChildren());
        }
        if (!(edit instanceof InsertEdit || edit instanceof MoveTargetEdit || edit instanceof CopyTargetEdit)) {
            textEdit.addField("length", edit.getLength());
        }
        if (edit instanceof ReplaceEdit) {
            textEdit.addField("text", ((ReplaceEdit)edit).getText());
            textEdit.addField("type", "ReplaceEdit");
        } else if (edit instanceof DeleteEdit) {
            textEdit.addField("type", "DeleteEdit");
        } else if (edit instanceof InsertEdit) {
            textEdit.addField("text", ((InsertEdit)edit).getText());
            textEdit.addField("type", "InsertEdit");
        } else if (edit instanceof CopyingRangeMarker) {
            textEdit.addField("type", "CopyingRangeMarker");
        } else if (edit instanceof CopySourceEdit) {
            if (((CopySourceEdit)edit).getTargetEdit() != null) {
                Jso copyTargetEdit = Jso.create();
                copyTargetEdit.addField("offSet", ((CopySourceEdit)edit).getTargetEdit().getOffset());
                textEdit.addField("CopyTargetEdit", copyTargetEdit);
            }
            textEdit.addField("type", "CopySourceEdit");
        } else if (edit instanceof MoveSourceEdit) {
            if (((MoveSourceEdit)edit).getTargetEdit() != null) {
                Jso moveTargetEdit = Jso.create();
                moveTargetEdit.addField("offSet", ((MoveSourceEdit)edit).getTargetEdit().getOffset());
                textEdit.addField("MoveTargetEdit", moveTargetEdit);
            }
            textEdit.addField("type", "MoveSourceEdit");
        } else if (edit instanceof MoveTargetEdit) {
            if (((MoveTargetEdit)edit).getSourceEdit() != null) {
                Jso moveSourceEdit = Jso.create();
                moveSourceEdit.addField("offSet", ((MoveTargetEdit)edit).getSourceEdit().getOffset());
                moveSourceEdit.addField("length", ((MoveTargetEdit)edit).getSourceEdit().getLength());
                textEdit.addField("MoveSourceEdit", moveSourceEdit);
            }
            textEdit.addField("type", "MoveTargetEdit");
        } else if (edit instanceof MultiTextEdit) {
            textEdit.addField("type", "MultiTextEdit");
        } else if (edit instanceof RangeMarker) {
            textEdit.addField("type", "RangeMarker");
        } else if (edit instanceof CopyTargetEdit) {
            Jso copySourceEdit = Jso.create();
            copySourceEdit.addField("offSet", ((CopyTargetEdit)edit).getSourceEdit().getOffset());
            copySourceEdit.addField("length", ((CopyTargetEdit)edit).getSourceEdit().getLength());

            textEdit.addField("CopySourceEdit", copySourceEdit);
            textEdit.addField("type", "CopyTargetEdit");
        }
        return textEdit;
    }

    private JsoArray<Jso> convertChildrenTextEditToJso(TextEdit[] childrensEdit) {
        JsoArray<Jso> edits = JsoArray.create();
        for (TextEdit chEdit : childrensEdit) {
            Jso child = convertTextEditToJso(chEdit);
            edits.add(child);
        }
        return edits;
    }

    private void initOptions() {
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.CORE_ENCODING, "UTF-8");
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        options.put(CompilerOptions.OPTION_TargetPlatform, JavaCore.VERSION_1_7);
        options.put(AssistOptions.OPTION_PerformVisibilityCheck, AssistOptions.ENABLED);
        options.put(CompilerOptions.OPTION_ReportUnusedLocal, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_TaskTags, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_ReportUnusedPrivateMember, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_SuppressWarnings, CompilerOptions.DISABLED);
        options.put(JavaCore.COMPILER_TASK_TAGS, "TODO,FIXME,XXX");
        options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        options.put(CompilerOptions.OPTION_Process_Annotations, JavaCore.DISABLED);

        options.put(AssistOptions.OPTION_PerformForbiddenReferenceCheck, AssistOptions.ENABLED);
        options.put(AssistOptions.OPTION_PerformVisibilityCheck, AssistOptions.ENABLED);
        options.put(AssistOptions.OPTION_PerformDeprecationCheck, AssistOptions.DISABLED);
        options.put(AssistOptions.OPTION_PerformDiscouragedReferenceCheck, AssistOptions.ENABLED);

    }

    /** {@inheritDoc} */
    @Override
    public void onMessage(MessageEvent event) {
        MessageImpl message = event.getDataAsJSO().cast();
        messageFilter.dispatchMessage(message);
    }

    public HashMap<String, String> getOptions() {
        return options;
    }

    @Override
    public void onMessageReceived(final ParseMessage message) {
                nameEnvironment.setProjectPath(message.projectPath());
                cuVar = new CUVariables(message.fileName(), message.packageName(), projectName);

                ASTParser parser = ASTParser.newParser(AST.JLS4);
                parser.setSource(message.source());
                parser.setKind(ASTParser.K_COMPILATION_UNIT);
                parser.setUnitName(message.fileName().substring(0, message.fileName().lastIndexOf('.')));
                parser.setResolveBindings(true);
                parser.setIgnoreMethodBodies(message.ignoreMethodBodies());
                parser.setNameEnvironment(nameEnvironment);
                ASTNode ast = parser.createAST();
                CompilationUnit unit = (CompilationUnit)ast;
                cuCache.putCompilationUnit(message.filePath(), unit, message.source());
                IProblem[] problems = unit.getProblems();
                MessagesImpls.ProblemsMessageImpl problemsMessage = MessagesImpls.ProblemsMessageImpl.make();
                JsoArray<Problem> problemsArray = JsoArray.create();
                for (IProblem p : problems) {
                    problemsArray.add(convertProblem(p));
                }
                IProblem[] tasks = (IProblem[])unit.getProperty("tasks");
                if (tasks != null) {
                    for (IProblem p : tasks) {
                        problemsArray.add(convertProblem(p));
                    }
                }
                problemsMessage.setProblems(problemsArray);
                problemsMessage.setId(message.id());
                worker.sendMessage(problemsMessage.serialize());
                outlineModelUpdater.onCompilationUnitChanged(unit, message.filePath());
    }

    private MessagesImpls.ProblemImpl convertProblem(IProblem p) {
        MessagesImpls.ProblemImpl problem = MessagesImpls.ProblemImpl.make();
        DefaultProblem prop = (DefaultProblem)p;

        problem.setOriginatingFileName(new String(prop.getOriginatingFileName()));
        problem.setMessage(prop.getMessage());
        problem.setId(prop.getID());
        problem.setStringArguments(from(prop.getArguments()));
        problem.setSeverity(prop.getSeverity());
        problem.setStartPosition(prop.getSourceStart());
        problem.setEndPosition(prop.getSourceEnd());
        problem.setLine(prop.getSourceLineNumber());
        problem.setColumn(prop.getSourceColumnNumber());

        return problem;
    }

    public ContentAssistHistory getContentAssistHistory() {
        if (contentAssistHistory == null) {
            Preferences preferences = new Preferences();
            // TODO use user name
            contentAssistHistory =
                    ContentAssistHistory.load(preferences, Preferences.CODEASSIST_LRU_HISTORY +"change me" /*userInfo.getName()*/);

            if (contentAssistHistory == null)
                contentAssistHistory = new ContentAssistHistory();
        }

        return contentAssistHistory;
    }

    /** @return  */
    public ContextTypeRegistry getTemplateContextRegistry() {
        if (fCodeTemplateContextTypeRegistry == null) {
            fCodeTemplateContextTypeRegistry = new ContextTypeRegistry();

            CodeTemplateContextType.registerContextTypes(fCodeTemplateContextTypeRegistry);
            JavaContextType contextTypeAll = new JavaContextType(JavaContextType.ID_ALL);

            contextTypeAll.initializeContextTypeResolvers();

            FieldResolver fieldResolver = new FieldResolver();
            fieldResolver.setType("field");
            contextTypeAll.addResolver(fieldResolver);

            LocalVarResolver localVarResolver = new LocalVarResolver();
            localVarResolver.setType("localVar");
            contextTypeAll.addResolver(localVarResolver);
            VarResolver varResolver = new VarResolver();
            varResolver.setType("var");
            contextTypeAll.addResolver(varResolver);
            NameResolver nameResolver = new NameResolver();
            nameResolver.setType("newName");
            contextTypeAll.addResolver(nameResolver);
            TypeResolver typeResolver = new TypeResolver();
            typeResolver.setType("newType");
            contextTypeAll.addResolver(typeResolver);
            ElementTypeResolver elementTypeResolver = new ElementTypeResolver();
            elementTypeResolver.setType("elemType");
            contextTypeAll.addResolver(elementTypeResolver);
            TypeVariableResolver typeVariableResolver = new TypeVariableResolver();
            typeVariableResolver.setType("argType");
            contextTypeAll.addResolver(typeVariableResolver);
            LinkResolver linkResolver = new LinkResolver();
            linkResolver.setType("link");
            contextTypeAll.addResolver(linkResolver);
            ImportsResolver importsResolver = new ImportsResolver();
            importsResolver.setType("import");
            StaticImportResolver staticImportResolver = new StaticImportResolver();
            staticImportResolver.setType("importStatic");
            contextTypeAll.addResolver(staticImportResolver);
            ExceptionVariableNameResolver exceptionVariableNameResolver = new ExceptionVariableNameResolver();
            exceptionVariableNameResolver.setType("exception_variable_name");
            contextTypeAll.addResolver(exceptionVariableNameResolver);
            fCodeTemplateContextTypeRegistry.addContextType(contextTypeAll);
            fCodeTemplateContextTypeRegistry.addContextType(new JavaDocContextType());
            JavaContextType contextTypeMembers = new JavaContextType(JavaContextType.ID_MEMBERS);
            JavaContextType contextTypeStatements = new JavaContextType(JavaContextType.ID_STATEMENTS);
            contextTypeMembers.initializeResolvers(contextTypeAll);
            contextTypeStatements.initializeResolvers(contextTypeAll);
            fCodeTemplateContextTypeRegistry.addContextType(contextTypeMembers);
            fCodeTemplateContextTypeRegistry.addContextType(contextTypeStatements);
        }

        return fCodeTemplateContextTypeRegistry;
    }

    /** @return  */
    public TemplateStore getTemplateStore() {
        if (templateStore == null)
            templateStore = new TemplateStore();
        return templateStore;
    }

    public INameEnvironment getNameEnvironment() {
        return nameEnvironment;
    }

    public CUVariables getCUVariables() {
        return cuVar;
    }
}
