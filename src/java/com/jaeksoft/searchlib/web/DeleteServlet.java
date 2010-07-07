/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2010 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.remote.StreamReadObject;
import com.jaeksoft.searchlib.request.DeleteRequest;
import com.jaeksoft.searchlib.request.SearchRequest;
import com.jaeksoft.searchlib.user.Role;
import com.jaeksoft.searchlib.user.User;

public class DeleteServlet extends AbstractServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2663934578246659291L;

	private int deleteUniqDoc(Client client, String indexName, String uniq)
			throws NoSuchAlgorithmException, IOException, URISyntaxException,
			SearchLibException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, HttpException {
		if (indexName == null)
			return client.deleteDocument(uniq) ? 1 : 0;
		else
			return client.deleteDocument(indexName, uniq) ? 1 : 0;
	}

	private int deleteUniqDocs(Client client, String indexName,
			Collection<String> uniqFields) throws NoSuchAlgorithmException,
			IOException, URISyntaxException, SearchLibException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		if (indexName == null)
			return client.deleteDocuments(uniqFields);
		else
			return client.deleteDocuments(indexName, uniqFields);
	}

	private int deleteByQuery(Client client, String q)
			throws CorruptIndexException, SearchLibException, IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, ParseException, SyntaxError,
			URISyntaxException, InterruptedException {
		SearchRequest request = client.getNewSearchRequest();
		request.setQueryString(q);
		return client.deleteDocuments(request);
	}

	@SuppressWarnings("unchecked")
	private int doObjectRequest(Client client, HttpServletRequest request,
			String indexName) throws ServletException {
		StreamReadObject readObject = null;
		try {

			readObject = new StreamReadObject(request.getInputStream());
			Object obj = readObject.read();
			if (obj instanceof DeleteRequest) {
				return deleteUniqDocs(client, indexName,
						((DeleteRequest<String>) obj).getCollection());
			} else if (obj instanceof String)
				return deleteUniqDoc(client, indexName, (String) obj);
			return 0;
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			if (readObject != null)
				readObject.close();
		}
	}

	@Override
	protected void doRequest(ServletTransaction transaction)
			throws ServletException {
		try {
			String indexName = transaction.getIndexName();
			User user = transaction.getLoggedUser();
			if (user != null && !user.hasRole(indexName, Role.INDEX_UPDATE))
				throw new SearchLibException("Not permitted");

			Client client = transaction.getClient();

			HttpServletRequest request = transaction.getServletRequest();
			String uniq = request.getParameter("uniq");
			String q = request.getParameter("q");
			Integer result = null;
			if (uniq != null)
				result = deleteUniqDoc(client, indexName, uniq);
			else if (q != null)
				result = deleteByQuery(client, q);
			else
				result = doObjectRequest(client, request, indexName);
			transaction.addXmlResponse("Status", "OK");
			transaction.addXmlResponse("Deleted", result.toString());
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public static boolean delete(URI uri, String indexName, String uniqueField)
			throws IOException, URISyntaxException, HttpException {
		String msg = call(buildUri(uri, "/delete", indexName, "uniq="
				+ uniqueField));
		return Boolean.parseBoolean(msg.trim());
	}

	public static int delete(URI uri, String indexName,
			Collection<String> uniqueFields) throws IOException,
			URISyntaxException {
		String msg = sendObject(buildUri(uri, "/delete", indexName, null),
				new DeleteRequest<String>(uniqueFields));
		return Integer.parseInt(msg.trim());
	}

	public static boolean deleteDocument(URI uri, String indexName, int docId)
			throws HttpException, IOException, URISyntaxException {
		String msg = call(buildUri(uri, "/delete", indexName, "id=" + docId));
		return Boolean.parseBoolean(msg.trim());
	}

	public static int deleteDocuments(URI uri, String indexName,
			Collection<Integer> docIds) throws IOException, URISyntaxException {
		String msg = sendObject(
				buildUri(uri, "/delete", indexName, "byId=yes"),
				new DeleteRequest<Integer>(docIds));
		return Integer.parseInt(msg.trim());
	}

}
