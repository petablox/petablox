/**
 * Modified example from page 100 of Lhotak's thesis.
 * 1-call-site sensitivity does not work for this example.
 */
public class ContainerTest3
{

  public static void main(String[] ps)
  {
    Item i1 = new Item();
    Container c1 = new Container(i1);

    Item i2 = new Item();           
    Container c2 = new Container(i2);
  }
}

class Container
{
  private Item item;

  public Container(Item item)
  {
    setItem(item);
  }

  void setItem(Item item)
  {
    this.item = item;
  }
}

class Item
{
}
