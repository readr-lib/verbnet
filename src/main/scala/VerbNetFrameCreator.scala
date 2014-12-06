import java.io.{File, FilenameFilter}
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import javax.xml.xpath.{XPath, XPathConstants, XPathFactory}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.w3c.dom.{Document, Element, NodeList}

import com.readr.client.meaning._
import com.readr.model.Project
import com.readr.model.annotation.{MentionAnn, TokenOffsetAnn, TextAnn, FrameMatchFeature}
import com.readr.model.frame._

import org.apache.commons.logging.LogFactory
import org.slf4j.LoggerFactory

//import com.readr.sparkclient.SparkClient._
//import com.readr.sparkclient.SparkClient

//import controllers.system.OperationCtrl._

object VerbNetFrameCreator {

  private var dir = "/Users/raphael/verbnet/new_vn"
  private var filename = dir + ""

  def run(implicit proj:Project, params:Map[String,String]):Unit = {

    val xmlFiles: Array[File] = new File(filename).listFiles(new FilenameFilter {
      @Override def accept(dir: File, name: String): Boolean = {
        return name.endsWith(".xml")
      }
    })
    for (xmlFile <- xmlFiles) process(xmlFile)
  }

  def process(xmlFile: File)(implicit proj:Project) {
    //implicit val ds = DataSource.defaultDataSource("readr_" + proj.ns + proj.proj)
    System.out.println("Reading " + xmlFile.getName)
    val dbFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val dBuilder: DocumentBuilder = dbFactory.newDocumentBuilder
    val doc: Document = dBuilder.parse(xmlFile)
    doc.getDocumentElement.normalize
    val xPath: XPath = XPathFactory.newInstance.newXPath
    val r: Element = xPath.evaluate("/VNCLASS", doc.getDocumentElement, XPathConstants.NODE).asInstanceOf[Element]
    readVnclass(r)
  }

  private def readVnclass(r: Element)(implicit p:Project):Future[Int] = {
    val ID = r.getAttribute("ID")
    val frame = new Frame("vn:" + ID)
    frames.create(frame) map {
      frameID =>
        read(r, frameID, frame)
        frameID
    }
  }

  private def read(r: Element, frameID:Int, frame:Frame)(implicit p:Project):Unit = {
    val ID = r.getAttribute("ID")
    val xPath: XPath = XPathFactory.newInstance.newXPath
    //frames.update(frameID, frame)
    frameProperties.add(frameID, new FrameProperty(-1, frameID, "Source", "VerbNet"))
    frameProperties.add(frameID, new FrameProperty(-1, frameID, "SourceKey", ID))
    val nodes2: NodeList = xPath.evaluate("THEMROLES/THEMROLE", r, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (i <- 0 until nodes2.getLength) {
      val s: Element = nodes2.item(i).asInstanceOf[Element]
      val `type` = s.getAttribute("type")
      frameArgs.add(frameID, new FrameArg(frameID, i.toByte, `type`, "", false))
      val nodes3: NodeList = xPath.evaluate("SELRESTRS/SELRESTR", s, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (j <- 0 until nodes3.getLength) {
        val t: Element = nodes3.item(j).asInstanceOf[Element]
        val Value = t.getAttribute("Value")
        val type3 = t.getAttribute("type")
      }
    }

    val sbExamples = new StringBuilder
    val nodes4: NodeList = xPath.evaluate("FRAMES/FRAME", r, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (i <- 0 until nodes4.getLength) {
      val s: Element = nodes4.item(i).asInstanceOf[Element]
      val d: Element = xPath.evaluate("DESCRIPTION", s, XPathConstants.NODE).asInstanceOf[Element]
      val descriptionNumber = d.getAttribute("descriptionNumber")
      val primary = d.getAttribute("primary")
      val secondary = d.getAttribute("secondary")
      val xtag = d.getAttribute("xtag")

      val nodes5: NodeList = xPath.evaluate("EXAMPLES/EXAMPLE", s, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (j <- 0 until nodes5.getLength) {
        val e: Element = nodes5.item(j).asInstanceOf[Element]
        val example = e.getTextContent
        sbExamples.append(example)
        sbExamples.append("\n")
      }

      val nodes6: NodeList = xPath.evaluate("SYNTAX/*", s, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (j <- 0 until nodes6.getLength) {
        val e: Element = nodes6.item(j).asInstanceOf[Element]
        val node = e.getNodeName
        val value = e.getAttribute("value")
        val nodes61: NodeList = xPath.evaluate("SYNRESTRS", s, XPathConstants.NODESET).asInstanceOf[NodeList]
        for (k <- 0 until nodes61.getLength) {
          val re: Element = nodes61.item(k).asInstanceOf[Element]
        }
      }

      val nodes7: NodeList = xPath.evaluate("SEMANTICS/PRED", s, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (j <- 0 until nodes7.getLength) {
        val e: Element = nodes7.item(j).asInstanceOf[Element]
        val bool = e.getAttribute("bool")
        val value = e.getAttribute("value")

        val nodes8: NodeList = xPath.evaluate("ARGS/ARG", e, XPathConstants.NODESET).asInstanceOf[NodeList]
        for (k <- 0 until nodes8.getLength) {
          val l: Element = nodes8.item(k).asInstanceOf[Element]
          val `type` = l.getAttribute("type")
          val value1 = l.getAttribute("value")
        }
      }
    }

    //frame = frame.copy(examples = sbExamples.toString)
    val nodes: NodeList = xPath.evaluate("MEMBERS/MEMBER", r, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (i <- 0 until nodes.getLength) {
      val s: Element = nodes.item(i).asInstanceOf[Element]
      val name = s.getAttribute("name")
      val wn = s.getAttribute("wn")
      val grouping = s.getAttribute("grouping")
      val memFrame = new Frame("vn:" + name)
      frames.create(memFrame) map {
        memFrameID =>
          frameRelations.inherit(frameID, memFrameID)
          frameProperties.add(memFrameID, new FrameProperty(-1, memFrameID, "wordnet", wn))
          frameProperties.add(memFrameID, new FrameProperty(-1, memFrameID, "ontonotes", grouping))
      }
    }

    val nodes9: NodeList = xPath.evaluate("SUBCLASSES/VNSUBCLASS", r, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (i <- 0 until nodes9.getLength) {
      val s: Element = nodes9.item(i).asInstanceOf[Element]
      readVnclass(s) map {
        subFrameID =>
          frameRelations.add(new FrameRelation(-1, frameID, subFrameID, FrameRelationType.VNSubClass, Seq[FrameRelationArg]()))
      }
      //val (subFrameID, subFrame): (Int,Frame) = readVnclass(s)
    }
    //return Tuple2(frameID, frame)
  }

  //val log = LogFactory.getLog(getClass)
  def logger = LoggerFactory.getLogger(getClass.getName)
}