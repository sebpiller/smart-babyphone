package ch.sebpiller.babyphone.toolkit.tensorflow;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;
import org.tensorflow.proto.*;

import java.io.Closeable;
import java.io.FileInputStream;
import java.nio.file.Path;

@Slf4j
public abstract class BaseTensorFlowRunnerFacade implements Closeable, AutoCloseable {
    protected final Graph graph;
    protected final Ops ops;
    protected final Session session;

    protected BaseTensorFlowRunnerFacade(Path modelPath) {
        if (modelPath == null) {
            graph = null;
            ops = null;
            session = null;
        } else {
            graph = new Graph();
            try (var is = new FileInputStream(modelPath.toFile())) {
                graph.importGraphDef(GraphDef.parseFrom(is));
            } catch (Exception e) {
                log.error("Failed to load model from {}", modelPath, e);
            }
            session = createNewSession(graph);
            ops = Ops.create(graph);
        }
    }

    @Override
    public void close() {
        log.debug("Closing TensorFlow session");
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed to close TensorFlow session", e);
            }
        }

        if (graph != null) {
            try {
                graph.close();
            } catch (Exception e) {
                log.error("Failed to close TensorFlow graph", e);
            }
        }
    }


    @NotNull
    protected Session createNewSession(Graph g) {
        var configProto = ConfigProto.getDefaultInstance().toBuilder()
                .setAllowSoftPlacement(true)
                .setLogDevicePlacement(true)
                .setUsePerSessionThreads(true)
                .setGraphOptions(GraphOptions.newBuilder()
                        .setOptimizerOptions(OptimizerOptions.getDefaultInstance().toBuilder()
                                .setDoFunctionInlining(true)
                                .setDoCommonSubexpressionElimination(true)
                                .setOptLevel(OptimizerOptions.Level.L1)
                                .setGlobalJitLevel(OptimizerOptions.GlobalJitLevel.ON_2)
                                .build())
                        .setPlacePrunedGraph(true)
                        .setEnableBfloat16Sendrecv(true)
                        .setInferShapes(true)
                        .setEnableRecvScheduling(true)
                        .setBuildCostModelAfter(10) // Random value
                        .setRewriteOptions(RewriterConfig.getDefaultInstance().toBuilder().setAutoParallel(AutoParallelOptions.getDefaultInstance())))
                .setInterOpParallelismThreads(12) // Random value
                .setIntraOpParallelismThreads(12)
                .build();

        return new Session(g, configProto);
    }


}
