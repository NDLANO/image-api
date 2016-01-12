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
  datasource.setUser(ImageApiProperties.MetaUserName)
  datasource.setPassword(ImageApiProperties.MetaPassword)
  datasource.setDatabaseName(ImageApiProperties.MetaResource)
  datasource.setServerName(ImageApiProperties.MetaServer)
  datasource.setPortNumber(ImageApiProperties.MetaPort)
  datasource.setInitialConnections(ImageApiProperties.MetaInitialConnections)
  datasource.setMaxConnections(ImageApiProperties.MetaMaxConnections)
  datasource.setCurrentSchema(ImageApiProperties.MetaSchema)

  def getImageStorageDefaultCredentials(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new ProfileCredentialsProvider())
    s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    new AmazonImageStorage(ImageApiProperties.StorageName, s3Client)
  }

  def getImageStorage(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new BasicAWSCredentials(ImageApiProperties.StorageAccessKey, ImageApiProperties.StorageSecretKey))
    s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    new AmazonImageStorage(ImageApiProperties.StorageName, s3Client)
  }

  def getImageMeta(): ImageMeta = {
    new PostgresMeta(datasource)
  }

  def getSearchMeta(): SearchMeta = {
    new ElasticSearchMeta(
      ImageApiProperties.SearchClusterName,
      ImageApiProperties.SearchHost,
      ImageApiProperties.SearchPort)
  }

  def getIndexMeta(): IndexMeta = {
    new ElasticIndexMeta(
      ImageApiProperties.SearchClusterName,
      ImageApiProperties.SearchHost,
      ImageApiProperties.SearchPort)
  }
}
