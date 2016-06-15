package de.jakobjarosch.rethinkdb.orm;

import com.google.auto.service.AutoService;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.rethinkdb.gen.ast.ReqlFunction1;
import com.rethinkdb.net.Connection;
import com.squareup.javapoet.*;
import de.jakobjarosch.rethinkdb.orm.annotation.PrimaryKey;
import de.jakobjarosch.rethinkdb.orm.annotation.RethinkDBModel;
import de.jakobjarosch.rethinkdb.orm.dao.GenericDAO;
import de.jakobjarosch.rethinkdb.orm.model.ChangeFeedElement;
import de.jakobjarosch.rethinkdb.orm.model.IndexModel;
import de.jakobjarosch.rethinkdb.orm.model.PrimaryKeyModel;
import rx.Observable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
public class RethinkDBDAOProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet(
                RethinkDBModel.class.getCanonicalName()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {
        try {
            process(roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Something went wrong: " + e);
        }
        return true;
    }

    private void process(RoundEnvironment roundEnv) throws Exception {
        for (final Element element : roundEnv.getElementsAnnotatedWith(RethinkDBModel.class)) {
            if (element instanceof TypeElement) {
                final TypeElement typeElement = (TypeElement) element;

                final RethinkDBModel modelAnnotation = typeElement.getAnnotation(RethinkDBModel.class);

                final PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement();

                final String packageName = packageElement.getQualifiedName().toString();
                final String daoClassName = typeElement.getSimpleName() + "DAO";
                final String daoQualifiedName = packageName.isEmpty() ? daoClassName : packageName + "." + daoClassName;

                final PrimaryKeyModel primaryKey = scanPrimaryKey(typeElement);
                final Set<IndexModel> indices = scanIndices(modelAnnotation);

                final JavaFileObject fileObject = processingEnv.getFiler().createSourceFile(daoQualifiedName);
                try (Writer w = fileObject.openWriter()) {

                    ClassName modelType = ClassName.get(typeElement);
                    ClassName listType = ClassName.get(List.class);
                    ClassName observableType = ClassName.get(Observable.class);
                    ClassName changeFeedElement = ClassName.get(ChangeFeedElement.class);
                    ClassName primaryKeyType = ClassName.get(primaryKey.getPackageName(), primaryKey.getClassName());
                    ClassName genericDaoType = ClassName.get(GenericDAO.class);
                    TypeName genericDaoGenericType = ParameterizedTypeName.get(genericDaoType, modelType, primaryKeyType);
                    TypeName listOfModelsType = ParameterizedTypeName.get(listType, modelType);
                    TypeName changeFeedType = ParameterizedTypeName.get(observableType, ParameterizedTypeName.get(changeFeedElement, modelType));

                    TypeSpec type = TypeSpec.classBuilder(daoClassName)
                            .addModifiers(Modifier.PUBLIC)

                            .addField(FieldSpec.builder(genericDaoGenericType, "dao", Modifier.PRIVATE, Modifier.FINAL).build())

                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(Connection.class, "connection")
                                    .addStatement("this.dao = new $T<$T, $T>(connection, $T.class, $S, $S)",
                                            genericDaoType, modelType, primaryKeyType, modelType, modelAnnotation.tableName(), primaryKey.getVariableName())
                                    .addCode(createIndiceCodeBlock(indices))
                                    .build())

                            .addMethod((MethodSpec.methodBuilder("initTable")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addStatement("this.dao.initTable()")
                                    .build()))

                            .addMethod((MethodSpec.methodBuilder("create")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(TypeName.get(typeElement.asType()), "model")
                                    .addStatement("this.dao.create(model)")
                                    .build()))

                            .addMethod(MethodSpec.methodBuilder("read")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(primaryKeyType, primaryKey.getVariableName())
                                    .returns(modelType)
                                    .addStatement("return this.dao.read($N)", primaryKey.getVariableName())
                                    .build())

                            .addMethod(MethodSpec.methodBuilder("read")
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(listOfModelsType)
                                    .addStatement("return this.dao.read()")
                                    .build())

                            .addMethod((MethodSpec.methodBuilder("read")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(ReqlFunction1.class, "filter")
                                    .returns(listOfModelsType)
                                    .addStatement("return this.dao.read(filter)")
                                    .build()))

                            .addMethod((MethodSpec.methodBuilder("update")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(TypeName.get(typeElement.asType()), "model")
                                    .addStatement("this.dao.update(model)")
                                    .build()))

                            .addMethod((MethodSpec.methodBuilder("delete")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(primaryKeyType, primaryKey.getVariableName())
                                    .addStatement("this.dao.delete($N)", primaryKey.getVariableName())
                                    .build()))

                            .addMethod((MethodSpec.methodBuilder("changes")
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(changeFeedType)
                                    .addStatement("return this.dao.changes()")
                                    .build()))

                            .addMethod((MethodSpec.methodBuilder("changes")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(ReqlFunction1.class, "filter")
                                    .returns(changeFeedType)
                                    .addStatement("return this.dao.changes(filter)")
                                    .build()))

                            .build();

                    JavaFile.builder(packageElement.getQualifiedName().toString(), type)
                            .build().writeTo(w);
                }
            }
        }
    }

    private CodeBlock createIndiceCodeBlock(Set<IndexModel> indices) {
        final CodeBlock.Builder builder = CodeBlock.builder();
        for (IndexModel index : indices) {
            builder.addStatement("this.dao.addIndex(" + index.isGeo() + ", $S)", Joiner.on(",").join(index.getFields()));
        }
        return builder.build();
    }

    private PrimaryKeyModel scanPrimaryKey(TypeElement element) {
        final List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        final Set<VariableElement> primaryKeys = variables.stream().filter(v -> v.getAnnotation(PrimaryKey.class) != null).collect(Collectors.toSet());

        if (primaryKeys.size() > 1) {
            log(Kind.ERROR, "Only one @PrimaryKey allowed.");
            throw new IllegalArgumentException();
        }

        return primaryKeys.stream().findFirst()
                .map(v -> new PrimaryKeyModel(getTypeElement(v.asType()), v.getSimpleName().toString()))
                .orElse(new PrimaryKeyModel("java.util", "String", "id"));
    }

    private Set<IndexModel> scanIndices(RethinkDBModel element) {
        return Arrays.stream(element.indices()).map(i -> new IndexModel(i.geo(), i.fields())).collect(Collectors.toSet());
    }

    private TypeElement getTypeElement(TypeMirror type) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(type);
    }

    private void log(Kind level, String message, Object... arguments) {
        processingEnv.getMessager().printMessage(level, String.format(message, arguments));
    }
}