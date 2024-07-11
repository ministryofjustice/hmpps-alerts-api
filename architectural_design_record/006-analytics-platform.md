# ADR006: Extracting prisoner alert data and sending to the Analytics Platform

## Summary of Data Extraction

The purpose of Data Extraction is to allow applications/services to share data to the Analytical Platform, so analysts can analyse application data easily, without having to access the application database.

## DPIA (Data Protection Impact Assessment)

The data protection impact assessment exists to cover the collection and processing of the data as part of the service. This is owned and completed by the dps team. When sending data to the analytics platform, this can be with or without a DPIA, but for production (live) data, a DPIA should be present.

## Issue with preprod

Unfortunately, at present, the pipeline configuration for preprod is not protected by the DPIA flags. In the HMPPS Alerts API, preprod data is populated by cloning data from production. This means we would be adding production data to a pipeline specifically configured to not require, nor have, a DPIA.

## Decision

A pipeline has been configured for dev, in order to provide data in an environment that can be developed against and test new functionality.
A pipeline has been configured for prod, covered by the DPIA.

After consideration, the decision was taken to not configure a pipeline for preprod until such time as the ability to configure the DPIA as required is implemented. At the time of writing, there was no immediate benefit to having a pipeline in preprod.