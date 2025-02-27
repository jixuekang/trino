/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.sql.planner.planprinter;

import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import io.trino.sql.planner.plan.PlanNodeId;

import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public class HashCollisionPlanNodeStats
        extends PlanNodeStats
{
    private final Map<String, OperatorHashCollisionsStats> operatorHashCollisionsStats;

    public HashCollisionPlanNodeStats(
            PlanNodeId planNodeId,
            Duration planNodeScheduledTime,
            Duration planNodeCpuTime,
            Duration planNodeBlockedTime,
            long planNodeInputPositions,
            DataSize planNodeInputDataSize,
            long planNodeOutputPositions,
            DataSize planNodeOutputDataSize,
            DataSize planNodeSpilledDataSize,
            Map<String, BasicOperatorStats> operatorStats,
            Map<String, OperatorHashCollisionsStats> operatorHashCollisionsStats)
    {
        super(
                planNodeId,
                planNodeScheduledTime,
                planNodeCpuTime,
                planNodeBlockedTime,
                planNodeInputPositions,
                planNodeInputDataSize,
                DataSize.ofBytes(0L),
                planNodeOutputPositions,
                planNodeOutputDataSize,
                planNodeSpilledDataSize,
                operatorStats);
        this.operatorHashCollisionsStats = requireNonNull(operatorHashCollisionsStats, "operatorHashCollisionsStats is null");
    }

    public Map<String, Double> getOperatorHashCollisionsAverages()
    {
        return operatorHashCollisionsStats.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getWeightedHashCollisions() / entry.getValue().getInputPositions()));
    }

    public Map<String, Double> getOperatorHashCollisionsStdDevs()
    {
        return operatorHashCollisionsStats.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> computedWeightedStdDev(
                                entry.getValue().getWeightedSumSquaredHashCollisions(),
                                entry.getValue().getWeightedHashCollisions(),
                                entry.getValue().getInputPositions())));
    }

    private static double computedWeightedStdDev(double sumSquared, double sum, double totalWeight)
    {
        double average = sum / totalWeight;
        double variance = (sumSquared - 2 * sum * average) / totalWeight + average * average;
        // variance might be negative because of numeric inaccuracy, therefore we need to use max
        return sqrt(max(variance, 0d));
    }

    public Map<String, Double> getOperatorExpectedCollisionsAverages()
    {
        return operatorHashCollisionsStats.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getWeightedExpectedHashCollisions() / entry.getValue().getInputPositions()));
    }

    @Override
    public PlanNodeStats mergeWith(PlanNodeStats other)
    {
        PlanNodeStats merged = super.mergeWith(other);

        return new HashCollisionPlanNodeStats(
                merged.getPlanNodeId(),
                merged.getPlanNodeScheduledTime(),
                merged.getPlanNodeCpuTime(),
                merged.getPlanNodeBlockedTime(),
                merged.getPlanNodeInputPositions(),
                merged.getPlanNodeInputDataSize(),
                merged.getPlanNodeOutputPositions(),
                merged.getPlanNodeOutputDataSize(),
                merged.getPlanNodeSpilledDataSize(),
                merged.operatorStats,
                operatorHashCollisionsStats);
    }
}
