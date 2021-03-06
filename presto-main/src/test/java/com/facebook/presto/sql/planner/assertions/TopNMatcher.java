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
package com.facebook.presto.sql.planner.assertions;

import com.facebook.presto.Session;
import com.facebook.presto.cost.PlanNodeCost;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.spi.block.SortOrder;
import com.facebook.presto.sql.planner.Symbol;
import com.facebook.presto.sql.planner.plan.PlanNode;
import com.facebook.presto.sql.planner.plan.TopNNode;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static com.facebook.presto.sql.planner.assertions.MatchResult.NO_MATCH;
import static com.facebook.presto.sql.planner.assertions.MatchResult.match;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

public class TopNMatcher
        implements Matcher
{
    private final long count;
    private final List<PlanTestSymbol> orderBySymbols;

    public TopNMatcher(long count, List<PlanTestSymbol> orderBySymbols)
    {
        this.count = count;
        this.orderBySymbols = ImmutableList.copyOf(orderBySymbols);
    }

    @Override
    public boolean shapeMatches(PlanNode node)
    {
        return node instanceof TopNNode;
    }

    @Override
    public MatchResult detailMatches(PlanNode node, PlanNodeCost planNodeCost, Session session, Metadata metadata, SymbolAliases symbolAliases)
    {
        checkState(shapeMatches(node), "Plan testing framework error: shapeMatches returned false in detailMatches in %s", this.getClass().getName());
        TopNNode topNNode = (TopNNode) node;

        if (topNNode.getCount() != count) {
            return NO_MATCH;
        }

        List<Symbol> expectedOrderBy = orderBySymbols.stream()
                .map(alias -> alias.toSymbol(symbolAliases))
                .collect(toImmutableList());

        if (!topNNode.getOrderBy().equals(expectedOrderBy)) {
            return NO_MATCH;
        }

        Map<Symbol, SortOrder> expectedOrderings = Maps.toMap(expectedOrderBy, Functions.constant(SortOrder.ASC_NULLS_FIRST));

        if (!topNNode.getOrderings().equals(expectedOrderings)) {
            return NO_MATCH;
        }

        return match();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("count", count)
                .add("orderBySymbols", orderBySymbols)
                .toString();
    }
}
