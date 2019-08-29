package no.ndla.imageapi.repository

import java.net.Socket

import no.ndla.imageapi.model.domain.ImageTitle
import no.ndla.imageapi.{DBMigrator, ImageApiProperties, IntegrationSuite, TestData, TestEnvironment}
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool}

import scala.util.{Success, Try}
import scalikejdbc._

class ImageRepositoryTest extends IntegrationSuite with TestEnvironment {

  val repository = new ImageRepository
  def databaseIsAvailable: Boolean = Try(repository.imageCount).isSuccess

  def emptyTestDatabase =
    DB autoCommit (implicit session => {
      sql"delete from imageapitest.imagemetadata;".execute.apply()(session)
    })

  override def beforeEach(): Unit =
    if (databaseIsAvailable) {
      emptyTestDatabase
    }

  override def beforeAll(): Unit =
    Try {
      val datasource = testDataSource.get
      if (serverIsListening) {
        ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
        DBMigrator.migrate(datasource)
      }
    }

  def serverIsListening: Boolean =
    Try(new Socket(ImageApiProperties.MetaServer, ImageApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
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

}