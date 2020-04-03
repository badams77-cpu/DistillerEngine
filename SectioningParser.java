
package SiteSearch;

public interface SectioningParser {

  public void firstSection();
  public boolean hasMoreSections();
  public String getSectionName();
  public int getSectionLength();
  public void nextSection() throws NoMoreSectionsException;

}