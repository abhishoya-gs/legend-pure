// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.io.Print;
import org.finos.legend.pure.runtime.java.interpreted.profiler.ActiveProfiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Profile extends NativeFunction
{
    private static final int PRINT_DEPTH = 10;

    private final Print print;
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public Profile(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
        this.print = new Print(functionExecution, repository);
    }

    @Override
    public boolean deferParameterExecution()
    {
        return true;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ActiveProfiler activeProfiler = new ActiveProfiler(processorSupport, Boolean.valueOf(params.get(1).getValueForMetaPropertyToOne(M3Properties.values).getName()));
        activeProfiler.start(functionExpressionToUseInStack);
        CoreInstance result = this.functionExecution.executeValueSpecification(params.get(0), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack,
                                                                                this.getParentOrEmptyVariableContext(variableContext), activeProfiler, instantiationContext, executionSupport);
        activeProfiler.end(functionExpressionToUseInStack);
        this.print.execute(FastList.newListWith(ValueSpecificationBootstrap.newStringLiteral(this.repository , activeProfiler.getReport(), processorSupport), ValueSpecificationBootstrap.newIntegerLiteral(this.repository, PRINT_DEPTH, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, context, processorSupport);

        CoreInstance profileResultType = processorSupport.package_getByUserPath("meta::pure::functions::tools::ProfileResult");
        CoreInstance profileResult = this.repository.newEphemeralAnonymousCoreInstance(null, profileResultType);

        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = this.repository.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), genericTypeType);
        Instance.addValueToProperty(classifierGenericType, M3Properties.rawType, profileResultType, processorSupport);
        CoreInstance T = Instance.extractGenericTypeFromInstance(result, processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, T, processorSupport);
        CoreInstance m = Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.multiplicity, processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.multiplicityArguments, m, processorSupport);

        Instance.addValueToProperty(profileResult, M3Properties.classifierGenericType, classifierGenericType, processorSupport);

        Instance.addValueToProperty(profileResult, "report", this.repository.newEphemeralCoreInstance(activeProfiler.getReport(), processorSupport.package_getByUserPath("String"), null), processorSupport);
        Instance.addValueToProperty(profileResult, "result", result.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);

        return  ValueSpecificationBootstrap.wrapValueSpecification(profileResult, true, processorSupport);
    }
}
