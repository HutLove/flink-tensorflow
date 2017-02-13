package org.apache.flink.contrib.tensorflow.examples.inception

import java.nio.file.Paths

import org.apache.flink.contrib.tensorflow.examples.inception.InceptionModel.LabeledImage
import org.apache.flink.contrib.tensorflow.streaming._
import org.apache.flink.streaming.api.functions.source.FileProcessingMode.PROCESS_ONCE
import org.apache.flink.streaming.api.scala._

import scala.concurrent.duration._
import ImageLabelingSignature._

/**
  * A streaming image labeler, based on the 'inception5h' model.
  */
object Inception {

  type Image = Array[Byte]

  def main(args: Array[String]) {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    if (args.length < 2) {
      System.out.println("Usage: Inception <model-dir> <images-dir>")
      System.exit(1)
    }
    val modelPath = Paths.get(args(0)).toUri
    val imagesPath = args(1)

    // read each image
    val imageStream = env
      .readFile(new ImageInputFormat, imagesPath, PROCESS_ONCE, (1 second).toMillis)

    // label each image tensor using the inception5h model
    val inceptionModel = new InceptionModel(modelPath)

    val labelStream: DataStream[(String,LabeledImage)] = imageStream
      .mapWithModel(inceptionModel) { (in, model) =>
        val labelTensor = model.label(in._2)
        (in._1, model.labeled(labelTensor).head)
      }

    labelStream.print()

    // execute program
    env.execute("Inception")
  }
}

