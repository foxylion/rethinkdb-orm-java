package de.jakobjarosch.rethinkdb.orm;

import com.google.auto.service.AutoService;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.rethinkdb.net.Connection;
import com.squareup.javapoet.*;
import de.jakobjarosch.rethinkdb.orm.annotation.PrimaryKey;
import de.jakobjarosch.rethinkdb.orm.annotation.RethinkDBModel;
import de.jakobjarosch.rethinkdb.orm.dao.GenericDAO;
import de.jakobjarosch.rethinkdb.orm.model.IndexModel;
import de.jakobjarosch.rethinkdb.orm.model.PrimaryKeyModel;
import de.jakobjarosch.rethinkdb.pool.PersistentConnection;
import de.jakobjarosch.rethinkdb.pool.RethinkDBPool;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Provider;
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
                    ClassName primaryKeyType = ClassName.get(primaryKey.getPackageName(), primaryKey.getClassName());
                    TypeName connectionProviderType = ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Connection.class));
                    ParameterizedTypeName genericDAO = ParameterizedTypeName.get(ClassName.get(GenericDAO.class), modelType, primaryKeyType);

                    TypeSpec type = TypeSpec.classBuilder(daoClassName)
                            .addModifiers(Modifier.PUBLIC)
                            .superclass(genericDAO)

                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(RethinkDBPool.class, "pool")
                                    .addStatement("this(() -> pool.getConnection())")
                                    .build())

                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(Connection.class, "connection")
                                    .addStatement("this(() -> new $T(connection))", PersistentConnection.class)
                                    .build())

                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(connectionProviderType, "connectionProvider")
                                    .addStatement("super(connectionProvider, $T.class, $S, $S)",
                                            modelType, modelAnnotation.tableName(), primaryKey.getVariableName())
                                    .addCode(createIndiceCodeBlock(indices))
                                    .build())

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
            builder.addStatement("addIndex(" + index.isGeo() + ", $S)", Joiner.on(",").join(index.getFields()));
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
                .orElse(new PrimaryKeyModel("java.lang", "String", "id"));
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