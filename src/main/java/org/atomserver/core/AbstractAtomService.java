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


package org.atomserver.core;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.CategoriesHandler;
import org.atomserver.VirtualWorkspaceHandler;
import org.atomserver.monitor.EntriesMonitor;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.uri.ServiceTarget;
import org.atomserver.uri.URIHandler;

import java.util.*;
import java.util.regex.Matcher;

/**
 * The abstract, base AtomService implementation. Subclasses must implement newAtomWorkspace().
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractAtomService implements AtomService {

    /**
     * Create an appropriate AtomWorkspace for this AtomService, delegating to the actual AtomService implementation.
     * The AtomWorkspace is created "empty" by the setWorkspaces method, which then delegates to the AtomWorkspace's
     * setOptions() and bootstrap() methods.
     *
     * @param parentService This AtomService.
     * @param name          The name associated with this AtomWorkspace
     * @return The newly created AtoWorkspace.
     */
    abstract public AtomWorkspace newAtomWorkspace(AtomService parentService, String name);

    static private final Log log = LogFactory.getLog(AbstractAtomService.class);

    static public final int DEFAULT_MAX_AGGREGATE_LINK_ENTRIES_PER_PAGE = 1000;
    static public final int DEFAULT_MAX_AGGREGATE_FULL_ENTRIES_PER_PAGE = 100;

    protected URIHandler uriHandler = null;
    protected java.util.Map<String, AtomWorkspace> workspaces = null;

    protected Map<String, VirtualWorkspaceHandler> virtualWorkspaceHandlers =
            new HashMap<String, VirtualWorkspaceHandler>();

    protected ServiceContext serviceContext;

    private int maxLinkAggregateEntriesPerPage = DEFAULT_MAX_AGGREGATE_LINK_ENTRIES_PER_PAGE;
    private int maxFullAggregateEntriesPerPage = DEFAULT_MAX_AGGREGATE_FULL_ENTRIES_PER_PAGE;

    private EntriesMonitor entriesMonitor = null;

    /**
     * {@inheritDoc}
     */
    public AtomWorkspace getAtomWorkspace(String workspace) {
        Matcher matcher = URIHandler.JOIN_WORKSPACE_PATTERN.matcher(workspace);
        return matcher.matches() ?
               getJoinWorkspace(joinWorkspaces(matcher.group(1))) :
               workspaces.get(workspace);
    }

    private List<String> joinWorkspaces(String commaSeparatedWorkspaceList) {
        return (List<String>) (commaSeparatedWorkspaceList == null ?
                               Collections.EMPTY_LIST :
                               Arrays.asList(commaSeparatedWorkspaceList.split("\\s*,\\s*")));
    }

    protected AtomWorkspace getJoinWorkspace(List<String> joinWorkspaces) {
        throw new UnsupportedOperationException("this service does not support join feeds.");
    }

    public void initialize() {}

    /**
     * {@inheritDoc}
     */
    public URIHandler getURIHandler() {
        return this.uriHandler;
    }

    /**
     * {@inheritDoc}
     */
    public void setUriHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
        this.uriHandler.setAtomService(this);
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceBaseUri() {
        return this.uriHandler.getServiceBaseUri();
    }

    public CategoriesHandler getCategoriesHandler() {
        return (CategoriesHandler)getVirtualWorkspaceHandler( VirtualWorkspaceHandler.CATEGORIES );
    }

    public void setVirtualWorkspaceHandlers(Map<String, VirtualWorkspaceHandler> virtualWorkspaceHandlers) {
        this.virtualWorkspaceHandlers = virtualWorkspaceHandlers;
    }

    public VirtualWorkspaceHandler getVirtualWorkspaceHandler( String id ) {
        return virtualWorkspaceHandlers.get(id);
    }

    public void addVirtualWorkspaceHandler( String id, VirtualWorkspaceHandler handler ) {
        virtualWorkspaceHandlers.put( id, handler );
    }    

    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    /**
     * Set the AtomWorkspaces for this AtomService. Probably this method is called from an IOC container like Spring.
     * An AtomWorkspace is created for each WorkspaceOptions object provided. the actual creation of the AtomWorkspace
     * is delgated to the newAtomWorkspace method, which created an "empty" AtomWorkspace. And then subsequently,
     * the AtomWorkspace is provisioned by calling first the setOptions method, and then the bootstrap method on
     * the newly created AtomWorkspace.
     *
     * @param workspaceOptionsSet The Set of WorkspaceOptions
     */
    public void setWorkspaces(java.util.Set<WorkspaceOptions> workspaceOptionsSet) {
        this.workspaces = new java.util.HashMap<String, AtomWorkspace>();
        for (WorkspaceOptions options : workspaceOptionsSet) {
            String workspaceName = options.getName();
            AtomWorkspace workspace = newAtomWorkspace(this, workspaceName);
            workspace.setOptions(options);
            workspace.bootstrap();
            this.workspaces.put(workspaceName, workspace);
        }
    }

    /**
     * Returns the actual number of Workspaces associated with this AtomService, including "invisible" Workspaces
     *
     * @return the number of workspaces
     */
    public int getNumberOfWorkspaces() {
        return (this.workspaces != null) ? this.workspaces.size() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumberOfVisibleWorkspaces() {
        int count = 0;
        for (AtomWorkspace wspace : workspaces.values()) {
            if (wspace.getOptions().isVisible()) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public java.util.Collection<String> listWorkspaces(RequestContext request) {
        IRI iri = request.getUri();
        if (log.isDebugEnabled()) {
            log.debug("listWorkspaces:: iri= " + iri);
        }
        ServiceTarget serviceTarget = uriHandler.getServiceTarget(request);

        String workspace = serviceTarget.getWorkspace();
        java.util.Collection<String> workspaceList = null;

        if (!StringUtils.isEmpty(workspace)) {
            // A workspace-specific Service doc was requested
            AtomWorkspace wspace = getAtomWorkspace(workspace);
            if (wspace != null) {
                workspaceList = java.util.Collections.singleton(wspace.getVisibleName());
            }
        } else {
            // A list of all workspaces was requested
            workspaceList = new java.util.ArrayList<String>();
            for (AtomWorkspace wspace : workspaces.values()) {
                if (wspace.getOptions().isVisible()) {
                    workspaceList.add(wspace.getName());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("listWorkspaces:: workspaceList= " + workspaceList);
        }
        return workspaceList;
    }


    /**
     * {@inheritDoc}
     */
    public void verifyURIMatchesStorage(String workspace, String collection, IRI iri, boolean checkIfCollectionExists) {

        if (workspace == null) {
            String msg = "The URL (" + iri + ") has a NULL workspace";
            log.error(msg);
            throw new BadRequestException(msg);
        }


        if (getAtomWorkspace(workspace) == null) {
            String msg = "The URL (" + iri + ") does not indicate a recognized Atom workspace (" + workspace + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        String wspace = getAtomWorkspace(workspace).getVisibleName();

        if (!workspaces.keySet().contains(wspace)) {
            String msg = "The URL (" + iri + ") does not indicate a recognized Atom workspace (" + workspace + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (collection == null) {
            String msg = "The URL (" + iri + ") has a NULL collection";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (!getAtomWorkspace(workspace).collectionExists(collection) && checkIfCollectionExists) {
            String msg = "The URL (" + iri + ") does not indicate an existing " +
                         "Atom collection (" + collection + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    public int getMaxLinkAggregateEntriesPerPage() {
        return maxLinkAggregateEntriesPerPage;
    }

    public int getMaxFullAggregateEntriesPerPage() {
        return maxFullAggregateEntriesPerPage;
    }

    public void setMaxLinkAggregateEntriesPerPage(int maxLinkAggregateEntriesPerPage) {
        this.maxLinkAggregateEntriesPerPage = maxLinkAggregateEntriesPerPage;
    }

    public void setMaxFullAggregateEntriesPerPage(int maxFullAggregateEntriesPerPage) {
        this.maxFullAggregateEntriesPerPage = maxFullAggregateEntriesPerPage;
    }


    public EntriesMonitor getEntriesMonitor() {
        return entriesMonitor;
    }

    public void setEntriesMonitor(EntriesMonitor entriesMonitor) {
        this.entriesMonitor = entriesMonitor;
    }
    
}
