package com.duowan.mobile.peiwan.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yy.core.yyp.smart.ISmartFlyperFactory;
import com.yy.core.yyp.smart.ParamEntity;
import com.yy.core.yyp.smart.SmartFlyperDelegate;
import com.yy.core.yyp.smart.WrapperMethod;
import com.yy.core.yyp.smart.anotation.LazyInit;
import com.yy.core.yyp.smart.anotation.SmartBroadCast;
import com.yy.core.yyp.smart.anotation.SmartJson;
import com.yy.core.yyp.smart.anotation.SmartMap;
import com.yy.core.yyp.smart.anotation.SmartParam;
import com.yy.core.yyp.smart.anotation.SmartUri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static javax.lang.model.element.ElementKind.INTERFACE;

@AutoService(Processor.class)
public class CustomProcessor extends AbstractProcessor {

    private Filer mFiler; //文件相关的辅助类
    private Elements mElementUtils; //元素相关的辅助类
    private Messager mMessager; //日志相关的辅助类
    private Map<TypeElement, List<ExecutableElement>> typeElementListMap;
    private static final String SUFFIX_CLASSNAME = "$$Delegate";
    private static final String OBSERVABLE_TYPE = "io.reactivex.Observable";
    private static final String BASEENTITY_TYPE = "com.yy.core.room.protocol.BaseEntity";
    private static final String SMARTOBSERVERRESULT_TYPE = "com.yy.core.yyp.smart.SmartObserverResult";
    private String moduleName = "App";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnv.getFiler();
        mElementUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
        typeElementListMap = new HashMap<>();
        Map<String, String> options = processingEnv.getOptions();
        if (options != null && !options.isEmpty()) {
            moduleName = options.get("moduleName");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(SmartBroadCast.class.getCanonicalName());
        types.add(SmartUri.class.getCanonicalName());
        return types;
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new LinkedHashSet<>();
        options.add("moduleName");
        return options;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "process...");
        if (!roundEnvironment.processingOver()) {
            parseAnonation(roundEnvironment);
        }
        return true;
    }

    private void parseAnonation(RoundEnvironment roundEnvironment) {
        Set<ExecutableElement> executableElements = ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(SmartUri.class));
        Set<ExecutableElement> executableElements2 = ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(SmartBroadCast.class));
        parse(executableElements);
        parse(executableElements2);
        generateDelegate();
    }

    private void parse(Set<ExecutableElement> executableElements) {
        if (executableElements.size() > 0) {
            for (ExecutableElement executableElement : executableElements) {
                TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
                TypeMirror typeMirror = typeElement.asType();
                if (!isInterface(typeMirror)) {
                    mMessager.printMessage(Diagnostic.Kind.ERROR, "only support interface");
                    return;
                }
                note("parseAnonation class=" + typeElement.getQualifiedName().toString());
                if (typeElementListMap.get(typeElement) != null) {
                    typeElementListMap.get(typeElement).add(executableElement);
                } else {
                    List<ExecutableElement> list = new ArrayList<>();
                    list.add(executableElement);
                    typeElementListMap.put(typeElement, list);
                }

            }
        }
    }

    private void generateDelegate() {
        if (!typeElementListMap.isEmpty()) {
            try {
                for (TypeElement typeElement : typeElementListMap.keySet()) {
                    List<MethodSpec> methodSpecList = new ArrayList<>();
                    List<ExecutableElement> executableElements = typeElementListMap.get(typeElement);
                    for (ExecutableElement executableElement : executableElements) {
                        SmartUri smartUri = executableElement.getAnnotation(SmartUri.class);
                        SmartBroadCast smartBroadCast = executableElement.getAnnotation(SmartBroadCast.class);
                        if (smartUri != null || smartBroadCast != null) {
                            List<ParameterSpec> parameterSpecs = new ArrayList<>();
                            for (VariableElement variableElement : executableElement.getParameters()) {
                                parameterSpecs.add(ParameterSpec.get(variableElement));
                            }
                            TypeMirror typeMirror = executableElement.getReturnType();

                            //取得方法参数列表
                            List<? extends VariableElement> methodParameters = executableElement.getParameters();
                            MethodSpec.Builder methdSpecBuilder = MethodSpec.methodBuilder(executableElement.getSimpleName().toString())
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(TypeName.get(typeMirror))
                                    .addJavadoc("apt自动生成的实现方法")
                                    .addParameters(parameterSpecs);
                            if (smartUri != null) {
                                generateSmartUriCode(smartUri, methdSpecBuilder);
                            }
                            if (smartBroadCast != null) {
                                generateSmartBroadcastCode(smartBroadCast, methodParameters, methdSpecBuilder);
                            }
                            //如果是类或接口类型,检验返回类型的正确性
                            checkAndGenerateCode(typeMirror, methdSpecBuilder);
                            //校验和生成方法形式参数类型
                            generateParamsCode(methodParameters, methdSpecBuilder);

                            //生成返回值
                            methdSpecBuilder.addStatement("return $T.send(wrapperMethod)", SmartFlyperDelegate.class);
                            methodSpecList.add(methdSpecBuilder.build());
                        }
                    }

                    String pkg = mElementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                    TypeSpec proxyClass = TypeSpec.classBuilder(typeElement.getSimpleName() + SUFFIX_CLASSNAME)
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(ClassName.get(typeElement))
                            .addMethods(methodSpecList)
                            .build();

                    JavaFile javaFile = JavaFile.builder(pkg, proxyClass)
                            .build();

                    javaFile.writeTo(mFiler);
                }
            } catch (Exception e) {
                warn("printing ,generateDelegate error " + e);
            }
            //生成api接口工厂
            generateFactory();
        }
    }

    private void generateFactory() {
        if (typeElementListMap.isEmpty()) {
            return;
        }
        try {
            FieldSpec.Builder fieldBuild = FieldSpec.builder(ParameterizedTypeName.get(LinkedHashMap.class, String.class, Object.class),
                    "apiMap", Modifier.PRIVATE);
            fieldBuild.initializer("new LinkedHashMap<>()");

            CodeBlock.Builder builder = CodeBlock.builder();
            for (TypeElement key : typeElementListMap.keySet()) {
                LazyInit lazyInit = key.getAnnotation(LazyInit.class);
                String cls = key.getQualifiedName() + SUFFIX_CLASSNAME;
                if (key.getNestingKind() == NestingKind.MEMBER) {
                    String pkg = processingEnv.getElementUtils().getPackageOf(key.getEnclosingElement()).toString();
                    cls = pkg + "." + key.getSimpleName() + SUFFIX_CLASSNAME;
                    note("allen-apt inner class " + key.getQualifiedName());
                }
                if (lazyInit != null) {
                    if (!lazyInit.value()) {
                        builder.addStatement("apiMap.put($S,new $L())", key.getQualifiedName().toString(),
                                cls);
                    }
                } else {
                    builder.addStatement("apiMap.put($S,new $L())", key.getQualifiedName().toString(),
                            cls);
                }
            }


            MethodSpec initApi = MethodSpec.methodBuilder("initApi")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addAnnotation(Override.class)
                    .addJavadoc("必须要初始化,保证api不为空")
                    .addCode(builder.build())
                    .build();

            MethodSpec init = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("$N()", initApi)
                    .build();

            MethodSpec getApi = MethodSpec.methodBuilder("getApi")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Object.class)
                    .addAnnotation(Override.class)
                    .addJavadoc("获取api,保证不会为空")
                    .addParameter(Class.class, "cls")
                    .addStatement("String clsName = cls.getCanonicalName();\n" +
                            "        Object api = apiMap.get(clsName);\n" +
                            "        if (api == null) {\n" +
                            "            try {\n" +
                            "                api = Class.forName(clsName + $S).newInstance();\n" +
                            "            }catch (java.lang.ClassNotFoundException ex) {\n" +
                            "                android.util.Log.e(\"SmartFlyperFactory\", \"start get inner class Delegate\");\n" +
                            "                clsName = clsName.substring(0, clsName.lastIndexOf(\".\"));\n" +
                            "                clsName = clsName.substring(0, clsName.lastIndexOf(\".\"));\n" +
                            "                try {\n" +
                            "                    api = Class.forName(clsName +\".\"+cls.getSimpleName()+ $S).newInstance();\n" +
                            "                } catch (Exception e1) {\n" +
                            "                    $L.e(\"SmartFlyperFactory\", \"getApi inner class error \", e1);\n" +
                            "                }                                       \n" +
                            "            } catch (Exception e) {\n" +
                            "                $L.e(\"SmartFlyperFactory\", \"getApi error \", e);\n" +
                            "            }\n" +
                            "            apiMap.put(clsName, api);\n" +
                            "        }\n" +
                            "        return api", SUFFIX_CLASSNAME, SUFFIX_CLASSNAME, "android.util.Log", "android.util.Log")
                    .build();

            MethodSpec removeApi = MethodSpec.methodBuilder("removeApi")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(Class.class, "cls")
                    .addJavadoc("移除api接口")
                    .returns(TypeName.BOOLEAN)
                    .addStatement("String clsName=cls.getCanonicalName();\n" +
                            "      if(apiMap.containsKey(clsName)){\n" +
                            "            apiMap.remove(clsName);\n" +
                            "            return true;\n" +
                            "        }\n" +
                            "        return false")
                    .build();

            TypeSpec factory = TypeSpec.classBuilder("SmartFlyperFactory$$" + moduleName)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(TypeName.get(ISmartFlyperFactory.class))
                    .addJavadoc("apt自动生成,不需要修改")
                    .addField(fieldBuild.build())
                    .addMethod(init)
                    .addMethod(initApi)
                    .addMethod(getApi)
                    .addMethod(removeApi)
                    .build();

            JavaFile javaFile = JavaFile.builder("com.yy.core.yyp.smart", factory)
                    .build();

            javaFile.writeTo(mFiler);
        } catch (Exception ex) {
            warn("printing ,generateFactory error " + ex.getMessage());
        }
    }

    private void checkAndGenerateCode(TypeMirror typeMirror, MethodSpec.Builder methdSpecBuilder) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            String erasureType = erasureType(typeMirror);//获取返回的类型
            DeclaredType declaredType = (DeclaredType) typeMirror;
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            //string,observale类型才合理
            if (TypeName.get(typeMirror).equals(TypeName.get(String.class))) {
                methdSpecBuilder.addStatement("wrapperMethod.returnTypeParams=$L.class",
                        TypeName.get(typeMirror));
            } else if (typeArguments.size() == 1) {
                if (erasureType.equals(OBSERVABLE_TYPE)) {
                    //判断参数是否为string或者BaseEntity或子类行
                    TypeMirror returnParameterType = typeArguments.get(0);
                    String erasureParamers = erasureType(returnParameterType);

                    if (erasureParamers.equals(String.class.getCanonicalName()) || erasureParamers.equals(BASEENTITY_TYPE) || isSubtypeOfType(returnParameterType, BASEENTITY_TYPE)) {
                        methdSpecBuilder.addStatement("wrapperMethod.returnTypeParams=$T.class",
                                TypeName.get(returnParameterType));
                    } else {
                        error("方法返回类型必须是Observable<T>,T is String or BaseEntity");
                    }
                } else {
                    error("方法返回类型必须是Observable<T>,T is String or BaseEntity");
                }
            } else {
                error("方法返回类型必须是Observable<T>,T is String or BaseEntity");
            }
        } else {
            error("方法返回类型必须是Observable<T>,T is String or BaseEntity");
        }
    }

    private void generateParamsCode(List<? extends VariableElement> methodParameters, MethodSpec.Builder methdSpecBuilder) {
        int size = methodParameters.size();
        methdSpecBuilder.addStatement("com.yy.core.yyp.smart.ParamEntity[] paramEntities = new com.yy.core.yyp.smart.ParamEntity[$L]", size);
        methdSpecBuilder.addStatement("Object[] args = new Object[$L]", size);

        for (int i = 0; i < size; i++) {
            VariableElement parameter = methodParameters.get(i);
            SmartParam smartParam = parameter.getAnnotation(SmartParam.class);
            SmartMap smartMap = parameter.getAnnotation(SmartMap.class);
            SmartJson smartJson = parameter.getAnnotation(SmartJson.class);
            if (smartParam != null) {
                methdSpecBuilder.addStatement("paramEntities[$L]=new $T($L, $S)", i, ParamEntity.class, ParamEntity.SMARTPARAM, smartParam.value());
            }
            if (smartMap != null) {
                methdSpecBuilder.addStatement("paramEntities[$L]=new $T($L, \"\")", i, ParamEntity.class, ParamEntity.SMARTMAP);
            }
            if (smartJson != null) {
                methdSpecBuilder.addStatement("paramEntities[$L]=new $T($L, \"\")", i, ParamEntity.class, ParamEntity.SMARTJSON);
            }
            methdSpecBuilder.addStatement("args[$L]=$L", i, parameter.getSimpleName().toString());
        }
        methdSpecBuilder.addStatement("wrapperMethod.args=args");
        methdSpecBuilder.addStatement("wrapperMethod.params=paramEntities");
    }

    private void generateSmartBroadcastCode(SmartBroadCast smartBroadCast, List<? extends VariableElement> methodParameters, MethodSpec.Builder methdSpecBuilder) {
        methdSpecBuilder.addStatement("$T wrapperMethod=new $T()", WrapperMethod.class, WrapperMethod.class)
                .addStatement("wrapperMethod.max=$L", smartBroadCast.max())
                .addStatement("wrapperMethod.min_rsp=$L", smartBroadCast.min())
                .addStatement("wrapperMethod.isSmartBroadcast=true");
        //获取广播中SmartObserverResult的参数类型
        if (methodParameters.size() != 1) {
            error("广播的参数必须只有一个");
        } else {
            TypeMirror varType = methodParameters.get(0).asType();
            if (varType instanceof TypeVariable) {
                note("smartflyper-apt is TypeVariable");
                TypeVariable typeVariable = (TypeVariable) varType;
                varType = typeVariable.getUpperBound();
            }
            //参数类型
            String methodParamErasure = erasureType(varType);
            if (methodParamErasure.equals(SMARTOBSERVERRESULT_TYPE)) {
                TypeMirror methodParamType = ((DeclaredType) varType).getTypeArguments().get(0);
                String methodParamTypeErasure = erasureType(methodParamType);
                if (methodParamTypeErasure.equals(String.class.getCanonicalName()) || methodParamTypeErasure.equals(BASEENTITY_TYPE) || isSubtypeOfType(methodParamType, BASEENTITY_TYPE)) {
                    methdSpecBuilder.addStatement("wrapperMethod.paramsTypes=$T.class",
                            TypeName.get(methodParamType));
                } else {
                    error("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型");
                }
            } else {
                error("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型");
            }
        }
    }

    private void generateSmartUriCode(SmartUri smartUri, MethodSpec.Builder methdSpecBuilder) {
        methdSpecBuilder.addStatement("$T wrapperMethod=new $T()", WrapperMethod.class, WrapperMethod.class)
                .addStatement("wrapperMethod.max=$L", smartUri.max())
                .addStatement("wrapperMethod.min_req=$L", smartUri.req())
                .addStatement("wrapperMethod.min_rsp=$L", smartUri.rsp());
    }

    private boolean isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType
                && ((DeclaredType) typeMirror).asElement().getKind() == INTERFACE;
    }

    private String erasureType(TypeMirror elementType) {
        String name = processingEnv.getTypeUtils().erasure(elementType).toString();
        int typeParamStart = name.indexOf('<');
        if (typeParamStart != -1) {
            name = name.substring(0, typeParamStart);
        }
        return name;
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (isTypeEqual(typeMirror, otherType)) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
    }


    private void note(String msg) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);

    }

    private void error(String msg) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    private void warn(String msg) {
        mMessager.printMessage(Diagnostic.Kind.WARNING, msg);

    }
}
