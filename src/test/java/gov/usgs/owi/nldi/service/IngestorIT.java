package gov.usgs.owi.nldi.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;


import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;

import gov.usgs.owi.nldi.BaseIT;
import gov.usgs.owi.nldi.dao.CrawlerSourceDao;
import gov.usgs.owi.nldi.dao.FeatureDao;
import gov.usgs.owi.nldi.dao.IngestDao;
import gov.usgs.owi.nldi.domain.CrawlerSource;
import gov.usgs.owi.nldi.springinit.DbTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,
	classes={DbTestConfig.class, IngestDao.class, FeatureDao.class,
	CrawlerSourceDao.class, CrawlerSource.class})
public class IngestorIT extends BaseIT {

	@Autowired
	private IngestDao ingestDao;
	@Autowired
	private FeatureDao featureDao;
	@Mock
	private HttpUtils httpUtils;

	private Ingestor ingestor;

	@Before
	public void initTest() {
		MockitoAnnotations.initMocks(this);
		ingestor = new Ingestor(ingestDao, featureDao, httpUtils);
	}

	@Test
	@DatabaseSetup("classpath:/cleanup/featureWqpTemp.xml")
	@DatabaseSetup("classpath:/testData/crawlerSource.xml")
	@ExpectedDatabase(
			table="nldi_data.feature_wqp",
			query="select crawler_source_id, identifier, name, uri, location, comid, st_x(location) long, st_y(location) lat from nldi_data.feature_wqp",
			value="classpath:/testResult/ingestorPointDbIntegration.xml",
			assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED)
	public void pointDbIntegrationTest() throws ClientProtocolException, IOException {
		URL url = this.getClass().getResource("/testData/wqp.geojson");
		when(httpUtils.callSourceSystem(any(CrawlerSource.class))).thenReturn(new File(url.getFile()));
		ingestor.ingest(1);
	}

	@Test
	@DatabaseSetup("classpath:/cleanup/featureNp21NwisTemp.xml")
	@DatabaseSetup("classpath:/testData/crawlerSource.xml")
	@ExpectedDatabase(
			table="nldi_data.feature_np21_nwis",
			query="select crawler_source_id, identifier, name, uri, location, comid, st_x(location) long, st_y(location) lat, reachcode, measure from nldi_data.feature_np21_nwis",
			value="classpath:/testResult/ingestorReachDbIntegration.xml",
			assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED)
	public void reachDbIntegrationTest() throws ClientProtocolException, IOException {
		URL url = this.getClass().getResource("/testData/np21Nwis.geojson");
		when(httpUtils.callSourceSystem(any(CrawlerSource.class))).thenReturn(new File(url.getFile()));
		ingestor.ingest(3);
	}

}
