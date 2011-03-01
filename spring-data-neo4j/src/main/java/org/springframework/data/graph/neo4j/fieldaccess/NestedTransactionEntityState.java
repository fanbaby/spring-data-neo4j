package org.springframework.data.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class NestedTransactionEntityState<ENTITY extends GraphBacked<STATE>, STATE> implements
        EntityState<ENTITY, STATE> {
    protected final EntityState<ENTITY, STATE> delegate;
    private final static Log log = LogFactory.getLog(NestedTransactionEntityState.class);
    private GraphDatabaseContext graphDatabaseContext;

    public NestedTransactionEntityState(final EntityState<ENTITY, STATE> delegate,
                                        GraphDatabaseContext graphDatabaseContext) {
        this.delegate = delegate;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public ENTITY getEntity() {
        return delegate.getEntity();
    }

    public void setPersistentState(STATE state) {
        delegate.setPersistentState(state);
    }

    @Override
    public Object getValue(Field field) {
        return delegate.getValue(field);
    }

    @Override
    public boolean isWritable(Field field) {
        return delegate.isWritable(field);
    }

    @Override
    public Object setValue(final Field field, final Object newVal) {
        return doInTransaction(new Callable<Object>() {
            public Object call() throws Exception {
                return delegate.setValue(field,newVal);
            }
        });
    }

    @Override
    public void createAndAssignState() {
        doInTransaction(new Callable<Void>() {
            public Void call() throws Exception {
                delegate.createAndAssignState();
                return null;
            }
        });
    }

    @Override
    public boolean hasPersistentState() {
        return delegate.hasPersistentState();
    }

    @Override
    public STATE getPersistentState() {
        return delegate.getPersistentState();
    }

    @Override
    public ENTITY persist() {
        return doInTransaction(new Callable<ENTITY>() {
            public ENTITY call() throws Exception {
                return delegate.persist();
            }
        });
    }

    protected <T> T doInTransaction(Callable<T> call) {
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            T result = call.call();
            tx.success();
            return result;
        } catch (Exception e) {
            tx.failure();
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }
}