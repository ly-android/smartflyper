package com.duowan.mobile.peiwan.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.yy.core.yyp.smart.ISmartFlyperFactory
import com.yy.core.yyp.smart.ParamEntity
import com.yy.core.yyp.smart.SmartFlyperDelegate
import com.yy.core.yyp.smart.WrapperMethod
import com.yy.core.yyp.smart.anotation.LazyInit
import com.yy.core.yyp.smart.anotation.SmartAppender
import com.yy.core.yyp.smart.anotation.SmartBroadCast
import com.yy.core.yyp.smart.anotation.SmartBroadCast2
import com.yy.core.yyp.smart.anotation.SmartJson
import com.yy.core.yyp.smart.anotation.SmartMap
import com.yy.core.yyp.smart.anotation.SmartParam
import com.yy.core.yyp.smart.anotation.SmartUri
import com.yy.core.yyp.smart.anotation.SmartUri2
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

@AutoService(Processor::class)
class CustomProcessor : AbstractProcessor() {
    private var mFiler //文件相关的辅助类
        : Filer? = null
    private var mElementUtils //元素相关的辅助类
        : Elements? = null
    private var mMessager //日志相关的辅助类
        : Messager? = null
    private var typeElementListMap: MutableMap<TypeElement, MutableList<ExecutableElement>>? = null
    private var moduleName: String? = "App"

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        mFiler = processingEnv.filer
        mElementUtils = processingEnv.elementUtils
        mMessager = processingEnv.messager
        typeElementListMap = HashMap()
        val options = processingEnv.options
        if (options != null && options.isNotEmpty()) {
            moduleName = options["moduleName"]
        }
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val types: MutableSet<String> = LinkedHashSet()
        types.add(SmartBroadCast::class.java.canonicalName)
        types.add(SmartBroadCast2::class.java.canonicalName)
        types.add(SmartUri::class.java.canonicalName)
        types.add(SmartUri2::class.java.canonicalName)
        return types
    }

    override fun getSupportedOptions(): Set<String> {
        val options: MutableSet<String> = LinkedHashSet()
        options.add("moduleName")
        return options
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        mMessager!!.printMessage(Diagnostic.Kind.NOTE, "process...")
        if (!roundEnvironment.processingOver()) {
            parseAnonation(roundEnvironment)
        }
        return true
    }

    private fun parseAnonation(roundEnvironment: RoundEnvironment) {
        val executableElements: MutableSet<ExecutableElement> =
            ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(
                SmartUri::class.java))
        val executableElements2: MutableSet<ExecutableElement> =
            ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(
                SmartBroadCast::class.java))
        val executableElements3: MutableSet<ExecutableElement> =
            ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(
                SmartUri2::class.java))
        val executableElements4: MutableSet<ExecutableElement> =
            ElementFilter.methodsIn(roundEnvironment.getElementsAnnotatedWith(
                SmartBroadCast2::class.java))
        executableElements3.addAll(executableElements)
        executableElements3.addAll(executableElements2)
        executableElements3.addAll(executableElements4)
        parse(executableElements3)
        typeElementListMap = typeElementListMap?.toSortedMap { o1, o2 ->
            o1?.qualifiedName.toString().compareTo(o2?.qualifiedName.toString())
        }
        generateDelegate()
    }

    private fun parse(executableElements: Set<ExecutableElement>) {
        if (executableElements.isNotEmpty()) {
            for (executableElement in executableElements) {
                val typeElement: TypeElement = executableElement.enclosingElement as TypeElement
                val typeMirror: TypeMirror = typeElement.asType()
                if (!isInterface(typeMirror)) {
                    mMessager!!.printMessage(Diagnostic.Kind.ERROR, "only support interface")
                    return
                }
                note("parseAnnotation class=" + typeElement.qualifiedName.toString())
                if (typeElementListMap!![typeElement] != null) {
                    typeElementListMap!![typeElement]!!.add(executableElement)
                } else {
                    val list: MutableList<ExecutableElement> = ArrayList()
                    list.add(executableElement)
                    typeElementListMap!![typeElement] = list
                }
            }
        }
    }

    private fun generateDelegate() {
        if (typeElementListMap!!.isNotEmpty()) {
            try {
                for (typeElement in typeElementListMap!!.keys) {
                    val methodSpecList: MutableList<FunSpec> = ArrayList()
                    val executableElements: List<ExecutableElement>? = typeElementListMap!![typeElement]
                    for (executableElement in executableElements!!) {
                        val smartUri: SmartUri? = executableElement.getAnnotation(SmartUri::class.java)
                        val smartUri2: SmartUri2? = executableElement.getAnnotation(SmartUri2::class.java)
                        val smartBroadCast: SmartBroadCast? = executableElement.getAnnotation(SmartBroadCast::class
                            .java)
                        val smartBroadCast2: SmartBroadCast2? =
                            executableElement.getAnnotation(SmartBroadCast2::class.java)
                        val smartAppender: SmartAppender? = executableElement.getAnnotation(SmartAppender::class.java)
                        if (smartUri != null || smartBroadCast != null || smartUri2 != null || smartBroadCast2 != null) {

                            val typeMirror: TypeMirror = executableElement.returnType
                            val isNullable = executableElement.getAnnotation(Nullable::class.java)

                            val typeName =
                                if (smartBroadCast2 != null || isNullable != null) {
                                    typeMirror.asTypeName().javaToKotlinType().copy(nullable = true)
                                } else
                                    typeMirror.asTypeName().javaToKotlinType()

                            if (smartBroadCast2 != null) {
                                note("allen-go $typeName")
                            }
                            //取得方法参数列表
                            val methodParameters: List<VariableElement> = executableElement.parameters
                            val methodSpecBuilder: FunSpec.Builder =
                                FunSpec.builder(executableElement.simpleName.toString())
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(typeName)
                                    .addKdoc("apt自动生成的实现方法")
                            if (smartUri != null) {
                                generateSmartUriCode(smartUri, methodSpecBuilder)
                            }
                            if (smartUri2 != null) {
                                generateSmartUri2Code(smartUri2, methodSpecBuilder)
                            }
                            if (smartBroadCast != null) {
                                generateSmartBroadcastCode(smartBroadCast, methodParameters, methodSpecBuilder)
                            }
                            if (smartBroadCast2 != null) {
                                generateSmartBroadcast2Code(smartBroadCast2, methodParameters, methodSpecBuilder)
                            }
                            //校验和生成方法形式参数类型
                            val returnTypeName =
                                generateParamsCode(methodParameters, isNullable, methodSpecBuilder, smartBroadCast2)
                            //如果是类或接口类型,检验返回类型的正确性
                            checkAndGenerateCode(typeMirror, returnTypeName, methodSpecBuilder)
                            if (smartAppender != null) {
                                methodSpecBuilder.addStatement("wrapperMethod.includeVersion=%L",
                                    smartAppender.includeVersion)
                                methodSpecBuilder.addStatement("wrapperMethod.includeUid=%L",
                                    smartAppender.includeUid)
                                methodSpecBuilder.addStatement("wrapperMethod.includePf=%L", smartAppender.includePf)
                            }
                            //生成返回值
                            if (smartUri != null || smartBroadCast != null) {
                                methodSpecBuilder.addStatement("return %T.send(wrapperMethod)",
                                    SmartFlyperDelegate::class.java)
                            } else if (smartUri2 != null) {
                                methodSpecBuilder.addStatement("return %T.sendCoroutines(wrapperMethod)",
                                    SmartFlyperDelegate::class.java)
                            } else if (smartBroadCast2 != null) {
                                methodSpecBuilder.addStatement("return %T.registerCoroutinesBroadcast(wrapperMethod)",
                                    SmartFlyperDelegate::class.java)
                            }
                            methodSpecList.add(methodSpecBuilder.build())
                        }
                    }
                    val pkg = mElementUtils!!.getPackageOf(typeElement).qualifiedName.toString()
                    val proxyClass: TypeSpec =
                        TypeSpec.classBuilder(typeElement.simpleName.toString() + SUFFIX_CLASSNAME)
                            .addModifiers(KModifier.PUBLIC)
                            .addSuperinterface(typeElement.asClassName())
                            .addFunctions(methodSpecList)
                            .build()
                    val javaFile: FileSpec = FileSpec.builder(pkg, proxyClass.name!!).addType(proxyClass)
                        .build()
                    javaFile.writeTo(mFiler!!)
                }
            } catch (e: Exception) {
                warn("printing ,generateDelegate error $e")
            }
            //生成api接口工厂
            generateFactory()
        }
    }

    private fun generateFunParams(
        variableElement: VariableElement,
        parameterSpecs: MutableList<ParameterSpec>
    ) {
        if (variableElement.asType().asTypeName() is ParameterizedTypeName) {
            var rawType =
                (variableElement.asType().asTypeName() as ParameterizedTypeName).rawType
            if (rawType == Continuation::class.asClassName()) {
                return
            }
        }
        //追加注解
        //                                val annotationMirrors = variableElement.annotationMirrors.filter {
        //                                    it.annotationType.toString() == SmartParam::class.qualifiedName
        //                                        || it.annotationType.toString() == SmartJson::class.qualifiedName
        //                                        || it.annotationType.toString() == SmartMap::class.qualifiedName
        //                                }
        var nullable = variableElement.getAnnotation(Nullable::class.java)
        //                                warn("variableElement=$nullable\n")
        //判断是否为mutable类型
        val smartParam: SmartParam? = variableElement.getAnnotation(SmartParam::class.java)
        val smartMap: SmartMap? = variableElement.getAnnotation(SmartMap::class.java)
        var paramTypeName = variableElement.asType().asTypeName()
        if (smartParam != null) {
            paramTypeName = paramTypeName.asMutableTypeName(smartParam)
        }
        if (smartMap != null) {
            paramTypeName = paramTypeName.asMutableTypeName2(smartMap)
        }
        var parameterSpecBuilder = ParameterSpec.builder(variableElement.simpleName.toString(),
            paramTypeName.javaToKotlinType())
            .jvmModifiers(variableElement.modifiers)
        if (nullable != null) {
            parameterSpecBuilder = ParameterSpec.builder(variableElement.simpleName.toString(),
                paramTypeName.javaToKotlinType().copy(nullable = true))
                .jvmModifiers(variableElement.modifiers)
        }
        //                                if (annotationMirrors.isNotEmpty()) {
        //                                    parameterSpecBuilder.addAnnotation(AnnotationSpec.Companion.get
        //                                    (annotationMirrors[0]))
        //                                }
        parameterSpecs.add(parameterSpecBuilder.build())
    }

    private fun TypeName.asMutableTypeName2(smartMap: SmartMap): TypeName {
        if (smartMap.isMutableMap) {
            return when (this) {
                is ParameterizedTypeName -> {
                    MUTABLE_MAP.parameterizedBy(
                        *typeArguments.map {
                            it.javaToKotlinType()
                        }.toTypedArray()
                    )
                }
                is WildcardTypeName -> {
                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlinType())
                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlinType())
                }

                else -> {
                    val className =
                        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()
                            ?.asString()
                    if (className == null) this
                    else ClassName.bestGuess(className)
                }
            }
        }
        return this
    }

    private fun TypeName.asMutableTypeName(smartParam: SmartParam): TypeName {
        if (smartParam.isMutableMap) {
            return when (this) {
                is ParameterizedTypeName -> {
                    MUTABLE_MAP.parameterizedBy(
                        *typeArguments.map {
                            it.javaToKotlinType()
                        }.toTypedArray()
                    )
                }
                is WildcardTypeName -> {
                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlinType())
                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlinType())
                }

                else -> {
                    val className =
                        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()
                            ?.asString()
                    if (className == null) this
                    else ClassName.bestGuess(className)
                }

            }
        } else if (smartParam.isMutableList) {
            return when (this) {
                is ParameterizedTypeName -> {
                    MUTABLE_LIST.parameterizedBy(
                        *typeArguments.map {
                            it.javaToKotlinType()
                        }.toTypedArray()
                    )
                }
                is WildcardTypeName -> {
                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlinType())
                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlinType())
                }

                else -> {
                    val className =
                        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()
                            ?.asString()
                    if (className == null) this
                    else ClassName.bestGuess(className)
                }
            }
        } else if (smartParam.isMutableSet) {
            return when (this) {
                is ParameterizedTypeName -> {
                    MUTABLE_SET.parameterizedBy(
                        *typeArguments.map {
                            it.javaToKotlinType()
                        }.toTypedArray()
                    )
                }
                is WildcardTypeName -> {
                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlinType())
                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlinType())
                }

                else -> {
                    val className =
                        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()
                            ?.asString()
                    if (className == null) this
                    else ClassName.bestGuess(className)
                }
            }
        }
        return this
    }

    private fun generateFactory() {
        if (typeElementListMap!!.isEmpty()) {
            return
        }
        try {

            val fieldBuild: PropertySpec.Builder = PropertySpec.builder("apiMap",
                LinkedHashMap::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class
                    .asClassName()),
                KModifier.PRIVATE)
            fieldBuild.initializer("LinkedHashMap()")
            val builder: CodeBlock.Builder = CodeBlock.builder()
            for (key in typeElementListMap!!.keys) {
                val lazyInit: LazyInit? = key.getAnnotation(LazyInit::class.java)
                var cls: String = key.qualifiedName.toString() + SUFFIX_CLASSNAME
                if (key.nestingKind == NestingKind.MEMBER) {
                    val pkg = processingEnv.elementUtils.getPackageOf(key.enclosingElement).toString()
                    cls = pkg + "." + key.simpleName + SUFFIX_CLASSNAME
                    note("allen-apt inner class " + key.qualifiedName)
                }
                if (lazyInit != null) {
                    if (!lazyInit.value) {
                        builder.addStatement("apiMap.put(%S,%L())", key.qualifiedName.toString(),
                            cls)
                    }
                } else {
                    builder.addStatement("apiMap.put(%S,%L())", key.qualifiedName.toString(),
                        cls)
                }
            }
            val initApi: FunSpec = FunSpec.builder("initApi")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .addKdoc("必须要初始化,保证api不为空")
                .addCode(builder.build())
                .build()
            val init: FunSpec = FunSpec.constructorBuilder()
                .addModifiers(KModifier.PUBLIC)
                .addStatement("%N()", initApi)
                .build()
            val getApi: FunSpec = FunSpec.builder("getApi")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .returns(Any::class.asClassName().copy(nullable = true))
                .addKdoc("获取api,可能为空")
                .addParameter("cls",
                    Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(Any::class)))
                .addStatement("var clsName = cls.getCanonicalName()!!\n" +
                    "        var api = apiMap.get(clsName)\n" +
                    "        return api")
                .build()
            val removeApi: FunSpec = FunSpec.builder("removeApi")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .addParameter("cls",
                    Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(Any::class)))
                .addKdoc("移除api接口")
                .returns(Boolean::class)
                .addStatement("var clsName=cls.getCanonicalName()\n" +
                    "      if(apiMap.containsKey(clsName)){\n" +
                    "            apiMap.remove(clsName)\n" +
                    "            return true\n" +
                    "        }\n" +
                    "        return false")
                .build()
            val factory: TypeSpec = TypeSpec.classBuilder("SmartFlyperFactory__$moduleName")
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(ISmartFlyperFactory::class)
                .addKdoc("apt自动生成,不需要修改")
                .addProperty(fieldBuild.build())
                .addFunction(init)
                .addFunction(initApi)
                .addFunction(getApi)
                .addFunction(removeApi)
                .build()
            val javaFile: FileSpec = FileSpec.builder("com.yy.core.yyp.smart", factory.name!!)
                .addType(factory)
                .build()
            javaFile.writeTo(mFiler!!)
        } catch (ex: Exception) {
            warn("printing ,generateFactory error " + ex.message)
        }
    }

    private val errorReturnInfo = "方法返回类型必须是String,BaseEntity,Observable<T>,T is String or BaseEntity"

    private fun checkAndGenerateCode(
        typeMirror: TypeMirror, returnTypeName: TypeName?, methdSpecBuilder: FunSpec
        .Builder
    ) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            val erasureType = erasureType(typeMirror) //获取返回的类型
            val declaredType: DeclaredType = typeMirror as DeclaredType
            val typeArguments: List<TypeMirror> = declaredType.typeArguments
            //string,observale类型才合理
            if (typeMirror.asTypeName() == String::class.java.asClassName()) {
                methdSpecBuilder.addStatement("wrapperMethod.returnTypeParams=%L::class.java",
                    typeMirror.asTypeName())
            } else if (typeMirror.asTypeName().javaToKotlinType() == Any::class.asClassName()) { //suspend函数
                note("checkAndGenerateCode ${typeMirror.asTypeName()} is suspend function ")
                methdSpecBuilder.addStatement("wrapperMethod.returnTypeParams=%L::class.java", returnTypeName
                    .toString().replace("?", "")) //删除可空类型的问号
            } else if (typeArguments.size == 1) {
                if (erasureType == OBSERVABLE_TYPE || erasureType == CHANNEL_TYPE) { //channel类型
                    //判断参数是否为string或者BaseEntity或子类行
                    val returnParameterType: TypeMirror = typeArguments[0]
                    val erasureParamers = erasureType(returnParameterType)
                    if (erasureParamers == String::class.java.canonicalName || erasureParamers == BASEENTITY_TYPE
                        || isSubtypeOfType(returnParameterType, BASEENTITY_TYPE)) {
                        methdSpecBuilder.addStatement("wrapperMethod.returnTypeParams=%T::class.java",
                            returnParameterType.asTypeName())
                    } else {
                        error(
                            " >>>>>1 typeMirror :$typeMirror , returnTypeName : $returnTypeName , methdSpecBuilder: $methdSpecBuilder")
                        error("$errorReturnInfo >>>>>1")
                    }
                } else {
                    error(
                        " >>>>>2 typeMirror :$typeMirror , returnTypeName : $returnTypeName , methdSpecBuilder: $methdSpecBuilder")
                    error("$errorReturnInfo >>>>>2")
                }
            } else {
                error(
                    " >>>>>3 typeMirror :$typeMirror , returnTypeName : $returnTypeName , methdSpecBuilder: $methdSpecBuilder")
                error("$errorReturnInfo >>>>>3")
            }
        } else {
            error(
                " >>>>>4 typeMirror :$typeMirror , returnTypeName : $returnTypeName , methdSpecBuilder: $methdSpecBuilder")
            error("$errorReturnInfo >>>>>4")
        }
    }

    private fun generateParamsCode(
        methodParameters: List<VariableElement>, nullable: Annotation?, methdSpecBuilder: FunSpec.Builder,
        smartBroadCast2: SmartBroadCast2?
    ): TypeName? {
        var returnTypeName: TypeName? = null
        val size = methodParameters.size
        val isSmartBroadCast2 = smartBroadCast2 != null
        if (!isSmartBroadCast2) {
            methdSpecBuilder.addStatement(
                "var paramEntities = arrayOfNulls<com.yy.core.yyp.smart.ParamEntity>(%L)", size)
            methdSpecBuilder.addStatement("var args = arrayOfNulls<Any>(%L)", size)
        }
        val parameterSpecs: MutableList<ParameterSpec> = ArrayList()
        for (i in 0 until size) {
            val parameter: VariableElement = methodParameters[i]
            val smartParam: SmartParam? = parameter.getAnnotation(SmartParam::class.java)
            val smartMap: SmartMap? = parameter.getAnnotation(SmartMap::class.java)
            val smartJson: SmartJson? = parameter.getAnnotation(SmartJson::class.java)
            if (smartParam != null) {
                methdSpecBuilder.addStatement("paramEntities[%L]=%T(%L, %S)", i, ParamEntity::class.java,
                    ParamEntity.SMARTPARAM, smartParam.value)
            }
            if (smartMap != null) {
                methdSpecBuilder.addStatement("paramEntities[%L]=%T(%L, \"\")", i, ParamEntity::class.java,
                    ParamEntity.SMARTMAP)
            }
            if (smartJson != null) {
                methdSpecBuilder.addStatement("paramEntities[%L]=%T(%L, \"\")", i, ParamEntity::class.java,
                    ParamEntity.SMARTJSON)
            }
            val varType = parameter.asType()

            //参数类型
            val methodParamErasure = erasureType(varType)
            if (methodParamErasure == CONTINUATION_TYPE) { //是kotlin suspends函数
                val methodParamType = (varType as DeclaredType).typeArguments[0]
//                warn("methodParamTypeErasure $methodParamType")
                val methodParamTypeErasure: String = erasureType2(methodParamType)
                if (nullable != null) {
                    returnTypeName =
                        ClassName.bestGuess(methodParamTypeErasure).javaToKotlinType().copy(nullable = true)
                    methdSpecBuilder.returns(returnTypeName)
                } else {
                    returnTypeName = ClassName.bestGuess(methodParamTypeErasure).javaToKotlinType()
                    methdSpecBuilder.returns(returnTypeName)
                }
                methdSpecBuilder.modifiers.remove(KModifier.PUBLIC)
                methdSpecBuilder.addModifiers(KModifier.SUSPEND)
            } else {
                methdSpecBuilder.addStatement("args[%L]=%L", i, parameter.simpleName.toString())
            }
            //生成方法参数列表
            generateFunParams(parameter, parameterSpecs)
        }
        if (!isSmartBroadCast2) {
            methdSpecBuilder.addParameters(parameterSpecs)
            methdSpecBuilder.addStatement("wrapperMethod.args=args")
            methdSpecBuilder.addStatement("wrapperMethod.params=paramEntities")
        }
        return returnTypeName
    }

    private fun generateSmartBroadcast2Code(
        smartBroadCast: SmartBroadCast2, methodParameters: List<VariableElement>, methdSpecBuilder: FunSpec.Builder
    ) {
        methdSpecBuilder.addStatement("var wrapperMethod=%T()", WrapperMethod::class)
            .addStatement("wrapperMethod.max=%L", smartBroadCast.max)
            .addStatement("wrapperMethod.min_rsp=%L", smartBroadCast.min)
            .addStatement("wrapperMethod.appId=%L", smartBroadCast.appId)
            .addStatement("wrapperMethod.sync=%L", smartBroadCast.sync)
            .addStatement("wrapperMethod.isSmartBroadcast=true")
        //获取广播中SmartObserverResult的参数类型
        if (methodParameters.isNotEmpty()) {
            error("广播不需要参数")
        }
    }

    private fun generateSmartBroadcastCode(
        smartBroadCast: SmartBroadCast, methodParameters: List<VariableElement>, methdSpecBuilder: FunSpec.Builder
    ) {
        methdSpecBuilder.addStatement("var wrapperMethod=%T()", WrapperMethod::class)
            .addStatement("wrapperMethod.max=%L", smartBroadCast.max)
            .addStatement("wrapperMethod.min_rsp=%L", smartBroadCast.min)
            .addStatement("wrapperMethod.appId=%L", smartBroadCast.appId)
            .addStatement("wrapperMethod.sync=%L", smartBroadCast.sync)
            .addStatement("wrapperMethod.isSmartBroadcast=true")
        //获取广播中SmartObserverResult的参数类型
        if (methodParameters.size != 1) {
            error("广播的参数必须只有一个")
        } else {
            var varType: TypeMirror = methodParameters[0].asType()
            if (varType is TypeVariable) {
                note("smartflyper-apt is TypeVariable")
                val typeVariable = varType as TypeVariable
                varType = typeVariable.upperBound
            }
            //参数类型
            val methodParamErasure = erasureType(varType)
            if (methodParamErasure == SMARTOBSERVERRESULT_TYPE) {
                val methodParamType: TypeMirror = (varType as DeclaredType).typeArguments[0]
                val methodParamTypeErasure = erasureType(methodParamType)
                if (methodParamTypeErasure == String::class.java.canonicalName
                    || methodParamTypeErasure == BASEENTITY_TYPE || isSubtypeOfType(
                        methodParamType, BASEENTITY_TYPE)) {
                    methdSpecBuilder.addStatement("wrapperMethod.paramsTypes=%T::class.java",
                        methodParamType.asTypeName())
                } else {
                    error("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型")
                }
            } else {
                error("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型")
            }
        }
    }

    private fun generateSmartUriCode(smartUri: SmartUri, methedSpecBuilder: FunSpec.Builder) {
        methedSpecBuilder.addStatement("var wrapperMethod=%T()", WrapperMethod::class)
            .addStatement("wrapperMethod.appId=%L", smartUri.appId)
            .addStatement("wrapperMethod.max=%L", smartUri.max)
            .addStatement("wrapperMethod.min_req=%L", smartUri.req)
            .addStatement("wrapperMethod.min_rsp=%L", smartUri.rsp)
            .addStatement("wrapperMethod.sync=%L", smartUri.sync)
    }

    private fun generateSmartUri2Code(smartUri: SmartUri2, methedSpecBuilder: FunSpec.Builder) {
        methedSpecBuilder.addStatement("var wrapperMethod=%T()", WrapperMethod::class)
            .addStatement("wrapperMethod.appId=%L", smartUri.appId)
            .addStatement("wrapperMethod.max=%L", smartUri.max)
            .addStatement("wrapperMethod.min_req=%L", smartUri.req)
            .addStatement("wrapperMethod.min_rsp=%L", smartUri.rsp)
            .addStatement("wrapperMethod.sync=%L", smartUri.sync)
    }

    private fun isInterface(typeMirror: TypeMirror): Boolean {
        return (typeMirror is DeclaredType
            && typeMirror.asElement().kind == ElementKind.INTERFACE)
    }

    private fun erasureType(elementType: TypeMirror): String {
        var name = processingEnv.typeUtils.erasure(elementType).toString()
        val typeParamStart = name.indexOf('<')
        if (typeParamStart != -1) {
            name = name.substring(0, typeParamStart)
        }
        return name
    }

    //泛型类型例如 ? super com.yy.core.room.protocol.BaseEntity
    private fun erasureType2(elementType: TypeMirror): String {
        var name = elementType.toString()
        name = name.replace("? super ", "")
        return name
    }

    private fun isSubtypeOfType(typeMirror: TypeMirror, otherType: String): Boolean {
        if (isTypeEqual(typeMirror, otherType)) {
            return true
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false
        }
        val declaredType: DeclaredType = typeMirror as DeclaredType
        val typeArguments: List<TypeMirror> = declaredType.typeArguments
        if (typeArguments.isNotEmpty()) {
            val typeString: StringBuilder = StringBuilder(declaredType.asElement().toString())
            typeString.append('<')
            for (i in typeArguments.indices) {
                if (i > 0) {
                    typeString.append(',')
                }
                typeString.append('?')
            }
            typeString.append('>')
            if (typeString.toString() == otherType) {
                return true
            }
        }
        val element: Element = declaredType.asElement()
        if (element !is TypeElement) {
            return false
        }
        val typeElement: TypeElement = element as TypeElement
        val superType: TypeMirror = typeElement.superclass
        if (isSubtypeOfType(superType, otherType)) {
            return true
        }
        for (interfaceType in typeElement.interfaces) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true
            }
        }
        return false
    }

    private fun note(msg: String) {
        mMessager!!.printMessage(Diagnostic.Kind.NOTE, msg)
    }

    private fun error(msg: String) {
        mMessager!!.printMessage(Diagnostic.Kind.ERROR, msg)
    }

    private fun warn(msg: String) {
        mMessager!!.printMessage(Diagnostic.Kind.WARNING, msg)
    }

    companion object {
        private const val SUFFIX_CLASSNAME = "ImplAutoGenerated"
        private const val OBSERVABLE_TYPE = "io.reactivex.Observable"
        private const val CHANNEL_TYPE = "kotlinx.coroutines.channels.Channel"
        private const val BASEENTITY_TYPE = "com.yy.core.room.protocol.BaseEntity"
        private const val SMARTOBSERVERRESULT_TYPE = "com.yy.core.yyp.smart.SmartObserverResult"
        private const val CONTINUATION_TYPE = "kotlin.coroutines.Continuation"
        private fun isTypeEqual(typeMirror: TypeMirror, otherType: String): Boolean {
            return otherType == typeMirror.toString()
        }
    }

    /**
     * 获取需要把java类型映射成kotlin类型的ClassName  如：java.lang.String 在kotlin中的类型为kotlin.String 如果是空则表示该类型无需进行映射
     */
    fun Element.javaToKotlinType(): ClassName? {
        val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asType().asTypeName().toString()))
            ?.asSingleFqName()?.asString()
        return if (className == null) {
            null
        } else {
            ClassName.bestGuess(className)
        }
    }

    val TypeName.kType
        get() = if (this.toString() == "java.lang.String") ClassName("kotlin", "String") else this

    fun TypeName.javaToKotlinType(): TypeName = when (this) {
        is ParameterizedTypeName -> {
            (rawType.javaToKotlinType() as ClassName).parameterizedBy(
                *typeArguments.map {
                    it.javaToKotlinType()
                }.toTypedArray()
            )
        }
        is WildcardTypeName -> {
            if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlinType())
            else WildcardTypeName.producerOf(outTypes[0].javaToKotlinType())
        }

        else -> {
            val className =
                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
            if (className == null) this
            else ClassName.bestGuess(className)
        }
    }
}