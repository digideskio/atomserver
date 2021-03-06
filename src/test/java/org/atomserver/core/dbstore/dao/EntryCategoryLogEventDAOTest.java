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

package org.atomserver.core.dbstore.dao;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.core.EntryMetaData;

import java.util.ArrayList;
import java.util.List;


public class EntryCategoryLogEventDAOTest
        extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(EntryCategoryLogEventDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
        categoryLogEventsDAO.deleteAllRowsFromEntryCategoryLogEvent();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------

    public void testCRUD() throws Exception {
        // COUNT
        int startCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        //String workspace = "widgets";
        String sysId = "acme";
        String propId = "2182";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        // INSERT the Entry
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);

        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

        // INSERT the EntryCategory
        EntryCategory entryIn = new EntryCategory();
        entryIn.setEntryStoreId(metaData.getEntryStoreId());
        entryIn.setScheme( scheme );
        entryIn.setTerm( term );

        int numRows = categoriesDAO.insertEntryCategory(entryIn);
        assertTrue(numRows > 0);

        EntryCategory entryOut = categoriesDAO.selectEntryCategory(entryIn);
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);

        // INSERT the EntryCategoryLogEvent
        numRows = categoryLogEventsDAO.insertEntryCategoryLogEvent(entryIn);
        assertTrue(numRows > 0);

        int count = categoryLogEventsDAO.getTotalCount(workspace);
        assertEquals((startCount + 1), count);

        // SELECT
        verifySelectLogEventBySchemeAndTerm( entryIn, 1, sysId, propId, scheme, term );
        verifySelectLogEventByScheme( entryIn, 1, sysId, propId, scheme );
        verifySelectLogEvent( entryIn, 1, sysId, propId, scheme );

        // DELETE
        entriesDAO.obliterateEntry(descriptor);

        // SELECT again
        List<EntryCategoryLogEvent> logEvents2 =
                categoryLogEventsDAO.selectEntryCategoryLogEventBySchemeAndTerm(entryIn);
        log.debug("====> logEvents2 = " + logEvents2);
        assertTrue(logEvents2.size() == 0);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testSelect() throws Exception {
        // COUNT
        int startCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        String propId = "31300";
        String scheme = "urn:ha/widgets";

        // INSERT the Entry
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null,0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);
        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

        // DELETE, then INSERT the Categories
        int numTags = 5;
        String[] terms = { "batman", "robin", "batman", "alfred", "robin" };
        EntryCategory[] entryIn = new EntryCategory[numTags];
        for ( int ii=0; ii< numTags; ii++ ) {
            entryIn[ii] = new EntryCategory();
            entryIn[ii].setEntryStoreId(metaData.getEntryStoreId());
            entryIn[ii].setScheme( scheme );
            entryIn[ii].setTerm( terms[ii]);

            categoriesDAO.deleteEntryCategory(entryIn[ii]);
            int numRows = categoriesDAO.insertEntryCategory(entryIn[ii]);
            assertTrue(numRows > 0);

            numRows = categoryLogEventsDAO.insertEntryCategoryLogEvent(entryIn[ii]);
            assertTrue(numRows > 0);
        }

        // SELECT the EntryCategoryLogEvent
        verifySelectLogEventBySchemeAndTerm( entryIn[0], 2, sysId, propId, scheme, "batman" );
        verifySelectLogEventBySchemeAndTerm( entryIn[1], 2, sysId, propId, scheme, "robin" );
        verifySelectLogEventBySchemeAndTerm( entryIn[3], 1, sysId, propId, scheme, "alfred" );

        // SELECT the others
        verifySelectLogEventByScheme( entryIn[0], 5, sysId, propId, scheme);

        verifySelectLogEvent( entryIn[0], 5, sysId, propId, scheme );

        // DELETE
        entriesDAO.obliterateEntry(descriptor);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testBatch() throws Exception {
        // COUNT
        int startCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        String propId = "30003";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        // INSERT the Entry
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);
        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

        // INSERT the Categories
        int numTags = 5;
        List<EntryCategory> ecList = new ArrayList();
        for ( int ii=0; ii< numTags; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(metaData.getEntryStoreId());
            entryIn.setScheme( scheme );
            entryIn.setTerm( term + ii );

            ecList.add( entryIn );
        }
        categoriesDAO.insertEntryCategoryBatch( ecList );

        // INSERT the LogEvents
        categoryLogEventsDAO.insertEntryCategoryLogEventBatch( ecList );

        int count = categoryLogEventsDAO.getTotalCount(workspace);
        assertEquals((startCount + numTags), count);
        
        verifySelectLogEvent( ecList.get(0), 5, sysId, propId, scheme );
        for ( int ii=0; ii< numTags; ii++ ) {
           verifySelectLogEventBySchemeAndTerm( ecList.get(ii), 1, sysId, propId, scheme, term + ii );
        }
        verifySelectLogEventByScheme( ecList.get(0), 5, sysId, propId, scheme );

        // DELETE
        entriesDAO.obliterateEntry(descriptor);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoryLogEventsDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    void verifySelectLogEventBySchemeAndTerm( EntryCategory tag, int size, String sysId, String propId,
                                              String scheme, String term ){
        assertEquals( term, tag.getTerm() );
        List<EntryCategoryLogEvent> logEvents =
                categoryLogEventsDAO.selectEntryCategoryLogEventBySchemeAndTerm(tag);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == size);

        for (EntryCategoryLogEvent logEvent : logEvents) {
            assertEquals(sysId, logEvent.getCollection());
            assertEquals(propId, logEvent.getEntryId());
            assertEquals(scheme, logEvent.getScheme());
            assertEquals(term, logEvent.getTerm());
            assertNotNull(logEvent.getCreateDate());
        }
    }

    void verifySelectLogEventByScheme( EntryCategory tag, int size, String sysId, String propId, String scheme){
        assertEquals( scheme, tag.getScheme() );
        List<EntryCategoryLogEvent> logEvents = categoryLogEventsDAO.selectEntryCategoryLogEventByScheme(tag);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == size);

        for (EntryCategoryLogEvent logEvent : logEvents) {
            assertEquals(sysId, logEvent.getCollection());
            assertEquals(propId, logEvent.getEntryId());
            assertEquals(scheme, logEvent.getScheme());
            assertNotNull(logEvent.getCreateDate());
        }
    }

    void verifySelectLogEvent( EntryCategory tag, int size, String sysId, String propId, String scheme ){
        List<EntryCategoryLogEvent> logEvents = categoryLogEventsDAO.selectEntryCategoryLogEvent(tag);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == size);

        for (EntryCategoryLogEvent logEvent : logEvents) {
            assertEquals(sysId, logEvent.getCollection());
            assertEquals(propId, logEvent.getEntryId());
            assertEquals(scheme, logEvent.getScheme());
            assertNotNull(logEvent.getCreateDate());
        }
    }

}
