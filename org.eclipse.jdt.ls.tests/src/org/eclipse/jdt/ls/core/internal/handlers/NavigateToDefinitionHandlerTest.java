/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.ClassFileUtil;
import org.eclipse.jdt.ls.core.internal.WorkspaceHelper;
import org.eclipse.jdt.ls.core.internal.managers.AbstractProjectsManagerBasedTest;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Fred Bricon
 *
 */
public class NavigateToDefinitionHandlerTest extends AbstractProjectsManagerBasedTest {

	private NavigateToDefinitionHandler handler;
	private IProject project;
	private IProject defaultProject;

	@Before
	public void setUp() throws Exception {
		handler = new NavigateToDefinitionHandler(preferenceManager);
		importProjects("maven/salut");
		project = WorkspaceHelper.getProject("salut");
		defaultProject = linkFilesToDefaultProject("singlefile/Single.java").getProject();
	}

	@Test
	public void testGetEmptyDefinition() throws Exception {
		List<? extends Location> definitions = handler.definition(
				new TextDocumentPositionParams(new TextDocumentIdentifier("/foo/bar"), new Position(1, 1)), monitor);
		assertNotNull(definitions);
		assertEquals(0, definitions.size());
	}

	@Test
	public void testAttachedSource() throws Exception {
		testClass("org.apache.commons.lang3.StringUtils", 20, 26);
	}

	@Test
	public void testNoClassContentSupport() throws Exception {
		when(preferenceManager.isClientSupportsClassFileContent()).thenReturn(true);
		String uri = ClassFileUtil.getURI(project, "org.apache.commons.lang3.StringUtils");
		when(preferenceManager.isClientSupportsClassFileContent()).thenReturn(false);
		List<? extends Location> definitions = handler.definition(new TextDocumentPositionParams(new TextDocumentIdentifier(uri), new Position(20, 26)), monitor);
		assertNotNull(definitions);
		assertEquals(0, definitions.size());
	}

	@Test
	public void testDisassembledSource() throws Exception {
		testClass("javax.tools.Tool", 6, 44);
	}

	@Test
	public void testJdkClasses() throws Exception {
		// because for test, we are using fake rt.jar(rtstubs.jar), the issue of issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=541573 will
		// never occur in test cases
		String uri = ClassFileUtil.getURI(defaultProject, "Single");
		TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri);
		handler.definition(new TextDocumentPositionParams(identifier, new Position(1, 31)), monitor);
		testClass("org.apache.commons.lang3.stringutils", 6579, 20);
	}

	private void testClass(String className, int line, int column) throws JavaModelException {
		String uri = ClassFileUtil.getURI(project, className);
		TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri);
		List<? extends Location> definitions = handler
				.definition(new TextDocumentPositionParams(identifier, new Position(line, column)), monitor);
		assertNotNull(definitions);
		assertEquals(1, definitions.size());
		assertNotNull(definitions.get(0).getUri());
		assertTrue(definitions.get(0).getRange().getStart().getLine() >= 0);
	}

}
