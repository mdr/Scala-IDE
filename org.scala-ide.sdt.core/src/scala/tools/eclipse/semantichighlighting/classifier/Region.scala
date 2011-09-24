package scala.tools.eclipse.semantichighlighting.classifier

case class Region(getOffset: Int, getLength: Int) {

  def intersects(other: Region) =
    !(other.getOffset >= getOffset + getLength || other.getOffset + other.getLength - 1 < getOffset)

}
