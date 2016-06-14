package de.jakobjarosch.rethinkdb.orm.model;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

public class PrimaryKeyModel {

    private final String packageName;
    private final String className;
    private final String variableName;

    public PrimaryKeyModel(TypeElement type, String variableName) {
        this.packageName = ((PackageElement) type.getEnclosingElement()).getQualifiedName().toString();
        this.className = type.getSimpleName().toString();
        this.variableName = variableName;
    }

    public PrimaryKeyModel(String packageName, String className, String variableName) {
        this.packageName = packageName;
        this.className = className;
        this.variableName = variableName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getVariableName() {
        return variableName;
    }
}
