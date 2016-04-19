/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

/**
 * The multi-thread runner.
 *
 * @param <T>
 */
public interface MultiThreadRunner<T> {

    void run(T input) throws Exception;
}
