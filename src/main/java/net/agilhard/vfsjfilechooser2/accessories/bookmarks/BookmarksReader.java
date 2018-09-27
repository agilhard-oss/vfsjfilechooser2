// CHECKSTYLE:OFF
/*
 *
 * Copyright (C) 2008-2009 Yves Zoundi
 * Copyright (C) 2008-2009 Stan Love
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
 * under the License.
 */
package net.agilhard.vfsjfilechooser2.accessories.bookmarks;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Utility class to read bookmarks
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @author Stan Love
 * @version 0.0.2
 */
final class BookmarksReader {
	private final List<TitledURLEntry> entries;

	/** The Logger. */
	private final Logger log = LoggerFactory.getLogger(BookmarksReader.class);

	@SuppressWarnings({ "synthetic-access", "resource" })
	public BookmarksReader(final File bookmarksFile) {
		entries = new ArrayList<TitledURLEntry>();
		Reader reader = null;
		try {
			final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			xmlReader.setContentHandler(new BookmarksHandler());

			reader = new BufferedReader(new InputStreamReader(new FileInputStream(bookmarksFile), "UTF-8"));

			// read 1st 2 bytes to support multiple encryptions
			final char[] code = new char[2];
			reader.read(code, 0, 2);
			log.debug("code=" + String.valueOf(code) + "=");
			if ((code[0] == 'b') && (code[1] == '1')) {
				log.debug("in encrypted code section");
				// read the encrypted file
				final InputStream is = new FileInputStream(bookmarksFile);

				int the_length = (int) bookmarksFile.length() - 2;
				log.debug("raw_length=" + (the_length + 2));
				if (the_length <= 0) {
					the_length = 1;
				}
				log.debug("fixed_length=" + the_length);
				final byte[] code2 = new byte[2];
				final byte[] outhex = new byte[the_length];
				try {
					is.read(code2);
					is.read(outhex);
					// is.read(outhex,2,the_length);
					is.close();
				} catch (final Exception e) {
					log.info("exception reading encrypted file" + e);
				}

				final byte[] out = Util.hexByteArrayToByteArray(outhex);

				// do the decryption

				final byte[] raw = new byte[16];
				raw[0] = (byte) 1;
				raw[2] = (byte) 23;
				raw[3] = (byte) 24;
				raw[4] = (byte) 2;
				raw[5] = (byte) 99;
				raw[6] = (byte) 200;
				raw[7] = (byte) 202;
				raw[8] = (byte) 209;
				raw[9] = (byte) 199;
				raw[10] = (byte) 181;
				raw[11] = (byte) 255;
				raw[12] = (byte) 33;
				raw[13] = (byte) 210;
				raw[14] = (byte) 214;
				raw[15] = (byte) 216;

				final SecretKeySpec skeyspec = new SecretKeySpec(raw, "Blowfish");
				final Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, skeyspec);
				final byte[] decrypted = cipher.doFinal(out);

				// convert decrypted into a bytestream and parse it
				final ByteArrayInputStream bstream = new ByteArrayInputStream(decrypted);
				final InputSource inputSource = new InputSource(bstream);
				xmlReader.parse(inputSource);
				log.debug("leaving encrypted code section");
			} else {
				log.debug("in decrypted code section");
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(bookmarksFile), "UTF-8"));
				final InputSource inputSource = new InputSource(reader);
				xmlReader.parse(inputSource);
				log.debug("leaving decrypted code section");
			}
		} catch (final SAXParseException e) {
			final StringBuilder sb = new StringBuilder();
			sb.append("Error parsing xml bookmarks file").append("\n").append(e.getLineNumber()).append(":")
					.append(e.getColumnNumber()).append("\n").append(e.getMessage());
			throw new RuntimeException(sb.toString(), e);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException("Bookmarks file doesn't exist!", e);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException ioe) {
					log.warn("Unable to close bookmarks stream", ioe);
				}
			}
		}
	}

	public List<TitledURLEntry> getParsedEntries() {
		return entries;
	}

	private class BookmarksHandler implements ContentHandler {

		@SuppressWarnings("unused")
		@Override
		public void setDocumentLocator(final Locator locator) {
			// .
		}

		@Override
		public void startDocument() throws SAXException {
			// .
		}

		@Override
		public void endDocument() throws SAXException {
			// .
		}

		@SuppressWarnings("unused")
		@Override
		public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
			// .
		}

		@SuppressWarnings("unused")
		@Override
		public void endPrefixMapping(final String prefix) throws SAXException {
			// .
		}

		@Override
		@SuppressWarnings({ "synthetic-access", "unused" })
		public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
				throws SAXException {
			if ("entry".equals(localName)) {
				TitledURLEntry tue = null;
				final String title = atts.getValue("title");
				final String url = atts.getValue("url");

				if ((title != null) && (url != null)) {
					tue = new TitledURLEntry(title, url);
					entries.add(tue);
				}
			}
		}

		@Override
		@SuppressWarnings("unused")
		public void endElement(final String uri, final String localName, final String qName) throws SAXException {
			// .
		}

		@Override
		@SuppressWarnings("unused")
		public void characters(final char[] ch, final int start, final int length) throws SAXException {
			// .
		}

		@Override
		@SuppressWarnings("unused")
		public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
			// .
		}

		@Override
		@SuppressWarnings("unused")
		public void processingInstruction(final String target, final String data) throws SAXException {
			// .
		}

		@SuppressWarnings("unused")
		@Override
		public void skippedEntity(final String name) throws SAXException {
			// .
		}
	}
}
// CHECKSTYLE:ON
