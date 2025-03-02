

# SnapRead

## Introduction

SnapRead is an AI powered image to text (OCR) tool. It helps you to extract text from a series of images.
The tool is built with human in the loop approach, where human can select a specific region of the image to extract 
text from and move to next image. This enables to quickly digitize text from photos made by phone, without needing 
pre-processing or cropping the image, making it easy to digitalize books or any other printed material.

### Running the tool

To operate this tool, you need to have an OpenAI API key. You need to set it into your computer `OPENAI_API_KEY` 
environment variable.

## Building the tool

Building this tool requires Java 23+ and Maven

Type `mvn clean package` to build. The tool is built into the `build/target/SnapRead/` directory

