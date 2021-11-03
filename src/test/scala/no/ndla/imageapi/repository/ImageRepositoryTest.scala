package no.ndla.imageapi.repository

import java.net.Socket

import no.ndla.imageapi.model.domain.ImageTitle
import no.ndla.imageapi.{DBMigrator, ImageApiProperties, TestData, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool}

import scala.util.{Success, Try}
import scalikejdbc._

class ImageRepositoryTest extends IntegrationSuite(EnablePostgresContainer = true) with UnitSuite with TestEnvironment {
  override val dataSource = testDataSource.get
  var repository: ImageRepository = _

  this.setDatabaseEnvironment()

  def serverIsListening: Boolean = {
    val server = ImageApiProperties.MetaServer
    val port = ImageApiProperties.MetaPort
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  def databaseIsAvailable: Boolean = {
    val res = Try(repository.imageCount)
    res.isSuccess
  }

  def emptyTestDatabase =
    DB autoCommit (implicit session => {
      sql"delete from imagemetadata;".execute()(session)
    })

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try {
      ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
      if (serverIsListening) {
        DBMigrator.migrate(dataSource)
      }
    }
  }

  override def beforeEach(): Unit = {
    repository = new ImageRepository
    if (databaseIsAvailable) { emptyTestDatabase }
  }

  test("That inserting and retrieving images works as expected") {
    assume(databaseIsAvailable)
    val image1 = TestData.bjorn.copy(id = None, titles = Seq(ImageTitle("KyllingFisk", "nb")))
    val image2 = TestData.bjorn.copy(id = None, titles = Seq(ImageTitle("Apekatter", "nb")))
    val image3 = TestData.bjorn.copy(id = None, titles = Seq(ImageTitle("Ruslebiff", "nb")))

    val id1 = repository.insert(image1).id.get
    val id2 = repository.insert(image2).id.get
    val id3 = repository.insert(image3).id.get

    repository.withId(id1).get should be(image1.copy(id = Some(id1)))
    repository.withId(id2).get should be(image2.copy(id = Some(id2)))
    repository.withId(id3).get should be(image3.copy(id = Some(id3)))
  }

  test("That fetching images based on path works") {
    assume(databaseIsAvailable)
    val path1 = "/path1.jpg"
    val path2 = "/path123.png"
    val path3 = "/path555.png"
    val image1 = TestData.bjorn.copy(id = None, imageUrl = path1)
    val image2 = TestData.bjorn.copy(id = None, imageUrl = path2)
    val image3 = TestData.bjorn.copy(id = None, imageUrl = path3)

    val id1 = repository.insert(image1).id.get
    val id2 = repository.insert(image2).id.get
    val id3 = repository.insert(image3).id.get

    repository.getImageFromFilePath(path1).get should be(image1.copy(id = Some(id1)))
    repository.getImageFromFilePath(path2).get should be(image2.copy(id = Some(id2)))
    repository.getImageFromFilePath(path3).get should be(image3.copy(id = Some(id3)))
    repository.getImageFromFilePath("/nonexistant.png") should be(None)
  }

  test("that fetching based on path works with and without slash") {
    assume(databaseIsAvailable)
    val path1 = "/path1.jpg"
    val image1 = TestData.bjorn.copy(id = None, imageUrl = path1)
    val id1 = repository.insert(image1).id.get

    val path2 = "path2.jpg"
    val image2 = TestData.bjorn.copy(id = None, imageUrl = path2)
    val id2 = repository.insert(image2).id.get

    repository.getImageFromFilePath(path1).get should be(image1.copy(id = Some(id1)))
    repository.getImageFromFilePath("/" + path1).get should be(image1.copy(id = Some(id1)))

    repository.getImageFromFilePath(path2).get should be(image2.copy(id = Some(id2)))
    repository.getImageFromFilePath("/" + path2).get should be(image2.copy(id = Some(id2)))
  }

  test("That fetching image from url where there exists multiple works") {
    assume(databaseIsAvailable)
    val path1 = "/path1.jpg"
    val image1 = TestData.bjorn.copy(id = None, imageUrl = path1)
    val id1 = repository.insert(image1).id.get
    val id2 = repository.insert(image1.copy(id = None)).id.get

    repository.getImageFromFilePath(path1).get should be(image1.copy(id = Some(id1)))
  }

  test("That fetching image from url with special characters are escaped") {
    assume(databaseIsAvailable)
    val path1 = "/path1.jpg"
    val image1 = TestData.bjorn.copy(id = None, imageUrl = path1)
    val id1 = repository.insert(image1).id.get

    val path2 = "/pa%h1.jpg"
    val image2 = TestData.bjorn.copy(id = None, imageUrl = path2)
    val id2 = repository.insert(image2).id.get

    repository.getImageFromFilePath(path1).get should be(image1.copy(id = Some(id1)))
    repository.getImageFromFilePath(path2).get should be(image2.copy(id = Some(id2)))
  }

}
