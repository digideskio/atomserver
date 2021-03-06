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
package org.atomserver.core.dbstore;


import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.utils.locale.LocaleUtils;

import java.io.File;

/**
 */
public class PostDBSTest extends CRUDDBSTestCase {

    private String currentLocale;
    private String currentWorkspace;

    public static Test suite()
    { return new TestSuite( PostDBSTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    {
        super.tearDown();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( getCurrentWorkspace(), "acme",
                                                                   getCurrentEntryId(),
                                                                   LocaleUtils.toLocale(getCurrentLocale())) );
        return entryIRI;
    }

    protected String getPropfileBase() {
        if ( currentLocale == null ) {
            return TEST_DATA_DIR + "/" + getCurrentWorkspace() + "/acme/" + getCurrentEntryId().substring(0,2) +
                   "/" + getCurrentEntryId() + "/" + getCurrentEntryId() + ".xml";
        } else {
            return TEST_DATA_DIR + "/" + getCurrentWorkspace() + "/acme/" + getCurrentEntryId().substring(0,2) +
                   "/" + getCurrentEntryId() + "/" + getCurrentLocale() + "/" +
                   getCurrentEntryId() + ".xml";
        }
    }

    protected File getEntryFile(int revision) throws Exception {
        return getEntryFile(getCurrentWorkspace(), "acme", getCurrentEntryId(),
                            currentLocale == null ? null : LocaleUtils.toLocale(currentLocale),
                            true, revision);
    }

    protected String getCurrentLocale() {
        return currentLocale;
    }

    protected void setCurrentLocale( String locale ) {
        this.currentLocale = locale;
    }

    protected String getCurrentWorkspace() {
        return currentWorkspace;
    }

    protected void setCurrentWorkspace( String workspace ) {
        this.currentWorkspace = workspace;
    }


    // --------------------
    //       tests
    //---------------------

    public void testCRUDNoLocale() throws Exception {

        setCurrentWorkspace( "dummy" );

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest( true, "dummy/acme", true, true, false, true, null );
        String selfLink = getSelfUriFromEditUri(finalEditLink);
        log.debug( "selfLink= " + selfLink);

        // SELECT against the just deleted entry
        ClientResponse response = clientGet( selfLink, null, 200, true );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();
        log.debug( "CONTENT= "+ entryOut.getContent() );
        assertTrue( entryOut.getContent().indexOf("<deletion") != -1);

        response.release();
        if (contentStorage instanceof FileBasedContentStorage) {
            int rev = extractRevisionFromURI( finalEditLink ) - 1;
            File pFile = getEntryFile(rev);
            assertTrue( pFile != null && pFile.exists() );
        }
    }

    public void testCRUDWithLocale() throws Exception {

        setCurrentLocale( "en" );
        setCurrentWorkspace( "widgets" );

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest( true, "widgets/acme", true, true, false, true, "en" );
        String selfLink = getSelfUriFromEditUri(finalEditLink);

        // SELECT against the just deleted entry
        ClientResponse response = clientGet( selfLink, null, 200, true );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();
        log.debug( "CONTENT= "+ entryOut.getContent() );
        assertTrue( entryOut.getContent().indexOf("<deletion") != -1);

        response.release();
        if (contentStorage instanceof FileBasedContentStorage) {
            int rev = extractRevisionFromURI( finalEditLink ) - 1;
            File pFile = getEntryFile(rev);
            assertTrue( pFile != null && pFile.exists() );
        }
    }


    public void testMinimalEntry() throws Exception {

        setCurrentLocale( "fr" );
        setCurrentWorkspace( "widgets" );
        
        String fullURL = getServerURL() + "widgets/acme";
        String urlToPost = fullURL + "?locale=fr";
        String id = null;

        //INSERT
        // Verify that the <id> is not required
        String editURI = post(id, urlToPost, getFileXMLInsert(), 201);

        fullURL = fullURL + "/" + getCurrentEntryId() + ".fr.xml";
        log.debug("fullURL = " + fullURL);

        log.debug("########################################## editURI = " + editURI);
        if (contentStorage instanceof FileBasedContentStorage) {
            File propFile = getEntryFile(0);
            assertNotNull(propFile);
            log.debug("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%propFile " + propFile);
            assertTrue(propFile.exists());
        }

        int rev = extractRevisionFromURI(editURI);
        assertEquals(1, rev);

        // SELECT
        String xmlTestString = "This is an insert";
        editURI = select(fullURL, true, 200, xmlTestString);
        log.debug("########################################## editURI = " + editURI);
        rev = extractRevisionFromURI(editURI);
        assertEquals(1, rev);
    }

}
