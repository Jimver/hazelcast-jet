/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.connector.map;

import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.impl.execution.init.Contexts.ProcSupplierCtx;
import com.hazelcast.jet.impl.processor.TransformBatchedP;
import com.hazelcast.jet.sql.impl.ExpressionUtil;
import com.hazelcast.jet.sql.impl.JetJoinInfo;
import com.hazelcast.jet.sql.impl.connector.keyvalue.KvRowProjector;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.query.impl.getters.Extractors;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

final class JoinScanProcessorSupplier implements ProcessorSupplier, DataSerializable {

    private static final int MAX_BATCH_SIZE = 1024;

    private JetJoinInfo joinInfo;
    private String mapName;
    private KvRowProjector.Supplier rightRowProjectorSupplier;

    private transient IMap<Object, Object> map;
    private transient InternalSerializationService serializationService;
    private transient Extractors extractors;

    @SuppressWarnings("unused")
    private JoinScanProcessorSupplier() {
    }

    JoinScanProcessorSupplier(
            JetJoinInfo joinInfo,
            String mapName,
            KvRowProjector.Supplier rightRowProjectorSupplier
    ) {
        this.joinInfo = joinInfo;
        this.mapName = mapName;
        this.rightRowProjectorSupplier = rightRowProjectorSupplier;
    }

    @Override
    public void init(@Nonnull Context context) {
        map = context.jetInstance().getMap(mapName);
        serializationService = ((ProcSupplierCtx) context).serializationService();
        extractors = Extractors.newBuilder(serializationService).build();
    }

    @Nonnull
    @Override
    public Collection<? extends Processor> get(int count) {
        List<Processor> processors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Processor processor = new TransformBatchedP<Object[], Object[]>(
                    MAX_BATCH_SIZE,
                    joinFn(joinInfo, map, rightRowProjectorSupplier.get(serializationService, extractors))
            ) {
                @Override
                public boolean isCooperative() {
                    return false;
                }
            };
            processors.add(processor);
        }
        return processors;
    }

    private static FunctionEx<List<? super Object[]>, Traverser<Object[]>> joinFn(
            JetJoinInfo joinInfo,
            IMap<Object, Object> map,
            KvRowProjector rightRowProjector
    ) {
        BiFunctionEx<Object[], Object[], Object[]> joinFn = ExpressionUtil.joinFn(joinInfo.condition());

        return lefts -> {
            List<Object[]> rows = new ArrayList<>();
            for (Entry<Object, Object> entry : map.entrySet()) {
                Object[] right = rightRowProjector.project(entry);
                if (right == null) {
                    continue;
                }

                for (Object left : lefts) {
                    Object[] joined = joinFn.apply((Object[]) left, right);
                    if (joined != null) {
                        rows.add(joined);
                    }
                }
            }
            return Traversers.traverseIterable(rows);
        };
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeObject(mapName);
        out.writeObject(rightRowProjectorSupplier);
        out.writeObject(joinInfo);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        mapName = in.readObject();
        rightRowProjectorSupplier = in.readObject();
        joinInfo = in.readObject();
    }
}