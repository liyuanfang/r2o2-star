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

package fantail.core;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class EvalTools {
    public static double computeScore(double[] actual, double[] pred) {
        
        //return computeKendallTau(actual, pred);
        return computeSpearmanCC(actual, pred);
        
        //return computeNDCGAtPosition(actual, pred, 3);
    }
    
    public static double computeKendallTau(double[] actual, double[] pred) {
        double[] aa = Tools.doubleArrayToRanking(actual);
        double[] pp = Tools.doubleArrayToRanking(pred);
        return Correlation.rankKendallTauBeta(aa, pp);
    }
    
    public static double computeSpearmanCC(double[] actual, double[] pred) {
        double[] aa = Tools.doubleArrayToRanking(actual);
        double[] pp = Tools.doubleArrayToRanking(pred);
        return Correlation.spearman(aa, pp);
        //SpearmansCorrelation sc = new SpearmansCorrelation();      
        //return sc.correlation(aa, pp);
    }
}
