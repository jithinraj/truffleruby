/*
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.truffleruby;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.truffleruby.core.kernel.TraceManager;
import org.truffleruby.core.module.ModuleFields;
import org.truffleruby.language.LazyRubyRootNode;
import org.truffleruby.language.RubyGuards;
import org.truffleruby.launcher.Launcher;
import org.truffleruby.launcher.options.OptionDescription;
import org.truffleruby.launcher.options.OptionsCatalog;
import org.truffleruby.platform.Platform;
import org.truffleruby.stdlib.CoverageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TruffleLanguage.Registration(
        name = "Ruby",
        id = Launcher.LANGUAGE_ID,
        version = Launcher.LANGUAGE_VERSION,
        mimeType = RubyLanguage.MIME_TYPE,
        dependentLanguages = "llvm")
@ProvidedTags({
        CoverageManager.LineTag.class,
        TraceManager.CallTag.class,
        TraceManager.ClassTag.class,
        TraceManager.LineTag.class,
        StandardTags.RootTag.class,
        StandardTags.StatementTag.class,
        StandardTags.CallTag.class
})
public class RubyLanguage extends TruffleLanguage<RubyContext> {

    public static final String PLATFORM = String.format("%s-%s", Platform.getArchitecture(), Platform.getOSName());

    public static final String MIME_TYPE = "application/x-ruby";
    public static final String EXTENSION = ".rb";

    public static final String SULONG_BITCODE_BASE64_MIME_TYPE = "application/x-llvm-ir-bitcode-base64";

    public static final String CEXT_MIME_TYPE = "application/x-ruby-cext-library";
    public static final String CEXT_EXTENSION = ".su";

    public final boolean SINGLE_THREADED = Boolean.getBoolean("truffleruby.single_threaded");

    @TruffleBoundary
    public static String fileLine(FrameInstance frameInstance) {
        if (frameInstance == null) {
            return "no frame";
        } else if (frameInstance.getCallNode() == null) {
            return "no call node";
        } else {
            final SourceSection sourceSection = frameInstance.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection == null) {
                return "no source section (" + frameInstance.getCallNode().getRootNode().getClass() + ")";
            } else {
                return fileLine(sourceSection);
            }
        }
    }

    @TruffleBoundary
    public static String fileLine(SourceSection section) {
        if (section == null) {
            return "no source section";
        } else {
            final Source source = section.getSource();

            final String path = source.getPath() != null ? source.getPath() : source.getName();
            if (section.isAvailable()) {
                return path + ":" + section.getStartLine();
            } else {
                return path;
            }
        }
    }

    @Override
    public RubyContext createContext(Env env) {
        Log.LOGGER.fine("createContext()");
        Launcher.printTruffleTimeMetric("before-create-context");
        // TODO CS 3-Dec-16 need to parse RUBYOPT here if it hasn't been already?
        final RubyContext context = new RubyContext(this, env);
        Launcher.printTruffleTimeMetric("after-create-context");
        return context;
    }

    @Override
    protected void initializeContext(RubyContext context) throws Exception {
        Log.LOGGER.fine("initializeContext()");
        Launcher.printTruffleTimeMetric("before-initialize-context");
        context.initialize();
        Launcher.printTruffleTimeMetric("after-initialize-context");
    }

    @Override
    protected boolean patchContext(RubyContext context, Env newEnv) {
        Log.LOGGER.fine("patchContext()");
        Launcher.printTruffleTimeMetric("before-patch-context");
        boolean patched = context.patch(newEnv);
        Launcher.printTruffleTimeMetric("after-patch-context");
        return patched;
    }

    @Override
    protected void finalizeContext(RubyContext context) {
        Log.LOGGER.fine("finalizeContext()");
        context.finalizeContext();
    }

    @Override
    protected void disposeContext(RubyContext context) {
        Log.LOGGER.fine("disposeContext()");
        context.disposeContext();
    }

    public static RubyContext getCurrentContext() {
        return getCurrentContext(RubyLanguage.class);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return Truffle.getRuntime().createCallTarget(new LazyRubyRootNode(this, null, null, request.getSource(), request.getArgumentNames()));
    }

    @Override
    protected Object findExportedSymbol(RubyContext context, String symbolName, boolean onlyExplicit) {
        final Object explicit = context.getInteropManager().findExportedObject(symbolName);

        if (explicit != null) {
            return explicit;
        }

        if (onlyExplicit) {
            return null;
        }

        return context.send(context.getCoreLibrary().getTruffleInteropModule(), "lookup_symbol", context.getSymbolTable().getSymbol(symbolName));
    }

    @Override
    protected Object getLanguageGlobal(RubyContext context) {
        return context.getCoreLibrary().getObjectClass();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return RubyGuards.isRubyBasicObject(object);
    }

    @Override
    protected String toString(RubyContext context, Object value) {
        if (value == null) {
            return "<null>";
        } else if (RubyGuards.isBoxedPrimitive(value) ||  RubyGuards.isRubyBasicObject(value)) {
            return context.send(value, "inspect").toString();
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return "<foreign>";
        }
    }

    @Override
    public Object findMetaObject(RubyContext context, Object value) {
        final Map<String, String> properties = new HashMap<>();
        final DynamicObject rubyClass = context.getCoreLibrary().getLogicalClass(value);
        final ModuleFields rubyClassFields = Layouts.CLASS.getFields(rubyClass);
        properties.put("type", rubyClassFields.getName());
        properties.put("className", rubyClassFields.getName());
        properties.put("description", toString(context, value));
        return new MetaObject(properties);
    }

    @Override
    protected SourceSection findSourceLocation(RubyContext context, Object value) {
        if (RubyGuards.isRubyModule(value)) {
            return Layouts.CLASS.getFields((DynamicObject) value).getSourceSection();
        } else if (RubyGuards.isRubyMethod(value)) {
            return Layouts.METHOD.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else if (RubyGuards.isRubyUnboundMethod(value)) {
            return Layouts.UNBOUND_METHOD.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else if (RubyGuards.isRubyProc(value)) {
            return Layouts.PROC.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else {
            return null;
        }
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        final List<OptionDescriptor> options = new ArrayList<>();

        for (OptionDescription<?> option : OptionsCatalog.allDescriptions()) {
            options.add(option.toDescriptor());
        }

        return OptionDescriptors.create(options);
    }

    @Override
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        if (SINGLE_THREADED) {
            return singleThreaded;
        } else {
            return true;
        }
    }

    @Override
    protected void initializeThread(RubyContext context, Thread thread) {
        if (thread == context.getThreadManager().getOrInitializeRootJavaThread()) {
            // Already initialized when creating the context
            return;
        }

        if (context.getThreadManager().isRubyManagedThread(thread)) {
            // Already initialized by the Ruby-provided Runnable
            return;
        }

        final DynamicObject foreignThread = context.getThreadManager().createForeignThread();
        context.getThreadManager().start(foreignThread, thread);
    }

    @Override
    protected void disposeThread(RubyContext context, Thread thread) {
        if (thread == context.getThreadManager().getRootJavaThread()) {
            // Let the context shutdown cleanup the main thread
            return;
        }

        if (context.getThreadManager().isRubyManagedThread(thread)) {
            // Already disposed by the Ruby-provided Runnable
            return;
        }

        final DynamicObject rubyThread = context.getThreadManager().getForeignRubyThread(thread);
        context.getThreadManager().cleanup(rubyThread, thread);
    }

    @MessageResolution(receiverType = MetaObject.class)
    static final class MetaObject implements TruffleObject {
        final Map<String, ? extends Object> properties;

        MetaObject(Map<String, ? extends Object> properties) {
            this.properties = properties;
        }

        static boolean isInstance(TruffleObject object) {
            return object instanceof MetaObject;
        }

        @Resolve(message = "READ")
        abstract static class ReadNode extends Node {
            @TruffleBoundary
            Object access(MetaObject obj, String name) {
                Object value = obj.properties.get(name);
                if (value == null) {
                    throw UnknownIdentifierException.raise(name);
                }
                return value;
            }
        }

        @Resolve(message = "KEYS")
        abstract static class KeysNode extends Node {
            @TruffleBoundary
            TruffleObject access(MetaObject obj) {
                return JavaInterop.asTruffleObject(obj.properties.keySet().toArray(new String[0]));
            }
        }

        @Resolve(message = "KEY_INFO")
        abstract static class KeyInfoNode extends Node {
            @TruffleBoundary
            int access(MetaObject obj, String name) {
                if (!obj.properties.containsKey(name)) {
                    return 0;
                }
                return KeyInfo.newBuilder().setReadable(true).setWritable(false).build();
            }
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return MetaObjectForeign.ACCESS;
        }
    }
}
