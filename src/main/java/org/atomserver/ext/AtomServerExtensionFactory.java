/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.atomserver.ext;

import org.apache.abdera.util.AbstractExtensionFactory;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.ext.category.CategoryOperation;
import org.atomserver.ext.batch.Status;
import org.atomserver.ext.batch.Operation;
import org.atomserver.ext.batch.Results;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomServerExtensionFactory extends AbstractExtensionFactory {
    public AtomServerExtensionFactory() {
        super(AtomServerConstants.ATOMSERVER_BATCH_NS);
        addImpl(AtomServerConstants.STATUS, Status.class);
        addImpl(AtomServerConstants.OPERATION, Operation.class);
        addImpl(AtomServerConstants.RESULTS, Results.class);
        addImpl(AtomServerConstants.CATEGORY_OP, CategoryOperation.class);
    }
}
