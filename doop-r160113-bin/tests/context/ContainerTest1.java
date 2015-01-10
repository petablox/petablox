/**
 * Example from page 100 of Lhotak's thesis.
 */
public class ContainerTest1
{
  public static void main(String[] ps)
  {
    Container c1 = new Container();
    Item i1 = new Item();
    c1.setItem(i1);
           
    Container c2 = new Container();
    Item i2 = new Item();
    c2.setItem(i2);   
  }
}

class Container
{
  private Item item;

  void setItem(Item item)
  {
    this.item = item;
  }
}

class Item
{
}
