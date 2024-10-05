/**
 * MIT License
 * Copyright (c) 2019 Montana State University Software Engineering Labs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package pique.runnable;

import pique.analysis.ITool;
import pique.calibration.IBenchmarker;
import pique.calibration.IWeighter;
import pique.calibration.WeightResult;
import pique.evaluation.Project;
import pique.model.Measure;
import pique.model.ModelNode;
import pique.model.QualityModel;
import pique.model.QualityModelImport;
import pique.utility.PiqueProperties;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

public abstract class AQualityModelDeriver {

    protected Project project;
    protected Properties properties;

    protected QualityModel qualityModelDesign;
    protected Set<ITool> tools;
    protected Path benchmarkRepository;
    protected String projectRootFlag;
    
    
    
    public QualityModel deriveModel(QualityModel qmDesign, Set<ITool> tools, Path benchmarkRepository, String projectRootFlag) {
    	
        // (1) Derive thresholds
        IBenchmarker benchmarker = qmDesign.getBenchmarker();
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDirectory);
        Map<String, BigDecimal[]> measureNameThresholdMappings = benchmarker.deriveThresholds(
            benchmarkRepository, qmDesign, tools, projectRootFlag);

        // (2) Elicitate weights
        IWeighter weighter = qmDesign.getWeighter();
        // TODO (1.0): Consider, instead of weighting all nodes in one sweep here, dynamically assigning IWeighter
        //  ojbects to each node to have them weight using JIT evaluation functions.
        Set<WeightResult> weights = weighter.elicitateWeights(qmDesign);
        // TODO: assert WeightResult names match expected TQI, QualityAspect, and ProductFactor names from quality model description

        // (3) Apply results to nodes in quality model by matching names
        // Thresholds (ProductFactor nodes)
        // TODO (1.0): Support now in place to apply thresholds to all nodes (if they exist), not just measures. Just
        //  need to implement.
        measureNameThresholdMappings.forEach((measureName, thresholds) -> {
            Measure measure = (Measure)qmDesign.getMeasure(measureName);
            measure.setThresholds(thresholds);
        });

        // Weights (TQI and QualityAspect nodes)
        weights.forEach(weightResult -> {
            Map<String, ModelNode> allNodes = qmDesign.getAllQualityModelNodes();
            allNodes.get(weightResult.getName()).setWeights(weightResult.getWeights());
        });

        return qmDesign;
    }
}
