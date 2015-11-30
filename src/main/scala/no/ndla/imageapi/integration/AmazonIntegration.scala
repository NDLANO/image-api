/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE 
 */
package no.ndla.imageapi.integration

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.business.{SearchMeta, ImageMeta, IndexMeta}
import org.postgresql.ds.PGPoolingDataSource


object AmazonIntegration {

  private val datasource = new PGPoolingDataSource()
  datasource.setUser(ImageApiProperties.get("META_USER_NAME"))
  datasource.setPassword(ImageApiProperties.get("META_PASSWORD"))
  datasource.setDatabaseName(ImageApiProperties.get("META_RESOURCE"))
  datasource.setServerName(ImageApiProperties.get("META_SERVER"))
  datasource.setPortNumber(ImageApiProperties.getInt("META_PORT"))
  datasource.setInitialConnections(ImageApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  datasource.setMaxConnections(ImageApiProperties.getInt("META_MAX_CONNECTIONS"))
  datasource.setCurrentSchema(ImageApiProperties.get("META_SCHEMA"))

  def getImageStorageDefaultCredentials(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new ProfileCredentialsProvider())
    s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    new AmazonImageStorage(ImageApiProperties.get("STORAGE_NAME"), s3Client)
  }

  def getImageStorage(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new BasicAWSCredentials(ImageApiProperties.get("STORAGE_ACCESS_KEY"), ImageApiProperties.get("STORAGE_SECRET_KEY")))
    s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    new AmazonImageStorage(ImageApiProperties.get("STORAGE_NAME"), s3Client)
  }

  def getImageMeta(): ImageMeta = {
    new PostgresMeta(datasource)
  }

  def getSearchMeta(): SearchMeta = {
    new ElasticSearchMeta(
      ImageApiProperties.get("SEARCH_CLUSTER_NAME"),
      ImageApiProperties.HostAddr,
      ImageApiProperties.get("SEARCH_PORT"))
  }

  def getSearchAdmin(searchMeta: SearchMeta) = {
    searchMeta match {
      case sa: IndexMeta => sa
      case _ => throw new ClassCastException
    }
  }

  def getIndexMeta(): IndexMeta = {
    new ElasticIndexMeta(
      ImageApiProperties.get("SEARCH_CLUSTER_NAME"),
      ImageApiProperties.HostAddr,
      ImageApiProperties.get("SEARCH_PORT"))
  }
}
