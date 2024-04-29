package com.vernalis.hellojchem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import org.zapodot.junit.db.annotations.EmbeddedDatabaseTest;
import org.zapodot.junit.db.annotations.EmbeddedDatabase;
import org.zapodot.junit.db.common.Engine;

import chemaxon.jchem.db.DatabaseProperties;
import chemaxon.jchem.db.DatabaseSearchException;
import chemaxon.jchem.db.Importer;
import chemaxon.jchem.db.Exporter;
import chemaxon.jchem.db.JChemSearch;
import chemaxon.jchem.db.StructureTableOptions;
import chemaxon.jchem.db.TableTypeConstants;
import chemaxon.jchem.db.Transfer;
import chemaxon.jchem.db.TransferException;
import chemaxon.jchem.db.UpdateHandler;
import chemaxon.util.ConnectionHandler;

@EmbeddedDatabaseTest(engine = Engine.HSQLDB, initialSqls = {
		// "CREATE TABLE PUBLIC.TEST_STRUCTURE_JCHEM(cd_id INTEGER PRIMARY KEY,
		// cd_structure binary, cd_smiles VARCHAR(1000), cd_formular VARCHAR(100));",
})

class AppTest {

	@EmbeddedDatabase
	private DataSource dataSource;

	ConnectionHandler getConnectionHandler() {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		ConnectionHandler connHandler = new ConnectionHandler(true);
		connHandler.setConnection(connection);

		return connHandler;
	}

	@BeforeEach
	void populateWithImport() throws SQLException, TransferException {

		ConnectionHandler connHandler = getConnectionHandler();

		// Create the jchemproperties table
		DatabaseProperties.createPropertyTable(connHandler);

		// Create the structure table
		StructureTableOptions tableOptions = new StructureTableOptions("structure",
				TableTypeConstants.TABLE_TYPE_MOLECULES);
		UpdateHandler.createStructureTable(connHandler, tableOptions);

		// Create some entries from an SDF file
		Importer importer = new Importer();

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("example.sdf").getFile());

		String fileName = file.getAbsolutePath();

		importer.setConnectionHandler(connHandler);
		importer.setInput(fileName);
		importer.setTableName("structure");

		importer.init();

		int imported = importer.importMols();

		System.out.println("Imported " + imported + " structures");

	}

	@Test
	void rawSMILESDump() throws SQLException {
		try (final Connection connection = dataSource.getConnection();
				final Statement statement = connection.createStatement();
				final ResultSet resultSet = statement.executeQuery("SELECT cd_smiles FROM structure;")) {

			assertEquals(true, resultSet.next());
			System.out.println(resultSet.getString(1));

		}

	}

	@Test
	void basicSearch() throws SQLException, IOException, DatabaseSearchException, TransferException {

		JChemSearch searcher = new JChemSearch();
		searcher.setStructureTable("structure");
		searcher.setQueryStructure("CC");
		searcher.setConnectionHandler(getConnectionHandler());

		searcher.run();

		assertEquals(1, searcher.getResultCount());

	}

	@Test
	void basicExport() throws SQLException, IOException, DatabaseSearchException, TransferException {

		String fileName = "example.smi";
		int format = Transfer.SMILES;
		OutputStream os = new FileOutputStream(new File(fileName));
		Exporter ex = new Exporter();
		ex.setConnectionHandler(getConnectionHandler());
		ex.setTableName("structure");
		// ex.setFieldList(fieldString);
		ex.setOutputStream(os);
		ex.setFormat(format);
		ex.setDefaults(true); // only exporting default fields

		System.out.println("Exporting structures into " + fileName + " ...");
		int written = ex.writeAll(); // writing file in one step
		System.out.println(written + " molecules were exported.");

	}

}
