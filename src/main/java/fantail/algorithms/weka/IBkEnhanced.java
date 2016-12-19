/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fantail.algorithms.weka;

import weka.core.Capabilities;
import weka.core.Instances;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class IBkEnhanced extends weka.classifiers.lazy.IBk {

    @Override
    public void buildClassifier(Instances instances) throws Exception {

        // can classifier handle the data?
        getCapabilities().testWithFail(instances);

        // remove instances with missing class
        instances = new Instances(instances);
        instances.deleteWithMissingClass();

        m_NumClasses = instances.numClasses();
        m_ClassType = instances.classAttribute().type();
        m_Train = new Instances(instances, 0, instances.numInstances());

        // Throw away initial instances until within the specified window size
        if ((m_WindowSize > 0) && (instances.numInstances() > m_WindowSize)) {
            m_Train = new Instances(m_Train,
                    m_Train.numInstances() - m_WindowSize,
                    m_WindowSize);
        }

        m_NumAttributesUsed = 0.0;
        for (int i = 0; i < m_Train.numAttributes(); i++) {
            if ((i != m_Train.classIndex())
                    && (m_Train.attribute(i).isNominal()
                    || m_Train.attribute(i).isNumeric())) {
                m_NumAttributesUsed += 1.0;
            }
        }

        m_NNSearch.setInstances(m_Train);

        // Invalidate any currently cross-validation selected k
        m_kNNValid = false;

        //m_defaultModel = new ZeroR();
        //m_defaultModel.buildClassifier(instances);
    }

    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capabilities.Capability.DATE_ATTRIBUTES);
        result.enable(Capabilities.Capability.MISSING_VALUES);

        // class
        result.enable(Capabilities.Capability.NOMINAL_CLASS);
        result.enable(Capabilities.Capability.NUMERIC_CLASS);
        result.enable(Capabilities.Capability.DATE_CLASS);
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
        
        // the weka default IBk doesn't support relational class 
        result.enable(Capabilities.Capability.RELATIONAL_CLASS);

        // instances
        result.setMinimumNumberInstances(0);

        return result;
    }
}
