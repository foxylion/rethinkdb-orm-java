package de.jakobjarosch.rethinkdb.orm.model.geo;

import com.rethinkdb.ast.ReqlAst;
import com.rethinkdb.gen.ast.ReqlFunction0;
import com.rethinkdb.gen.proto.TermType;
import com.rethinkdb.model.Arguments;
import com.rethinkdb.model.OptArgs;

import java.util.List;
import java.util.Map;


public abstract class ReqlGeo extends ReqlAst {

    protected ReqlGeo(TermType termType, Arguments args, OptArgs optargs) {
        super(termType, args, optargs);
    }
}
