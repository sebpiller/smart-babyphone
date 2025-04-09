package ch.sebpiller.babyphone.tensorflow;

import lombok.extern.slf4j.Slf4j;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;
import org.tensorflow.proto.ConfigProto;
import org.tensorflow.proto.GraphDef;

import java.io.Closeable;
import java.io.FileInputStream;

@Slf4j
public abstract class BaseTensorFlowRunnerFacade implements Closeable, AutoCloseable {
    //  protected final SavedModelBundle model;
    protected final Graph graph;
    protected final Ops ops;
    protected final Session session;

    protected BaseTensorFlowRunnerFacade(String modelPath) {
        graph = new Graph();

        if (modelPath != null)
            try (var ia = new FileInputStream(modelPath)) {
                graph.importGraphDef(GraphDef.parseFrom(ia));
            } catch (Exception e) {
                log.error("Failed to load model from {}", modelPath, e);
            }

        session = new Session(graph, ConfigProto.newBuilder()
                .setAllowSoftPlacement(true)
                .build());
        ops = Ops.create(graph);
    }

    @Override
    public final void close() {
        graph.close();
    }


}
