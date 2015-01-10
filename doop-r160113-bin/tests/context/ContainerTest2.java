/**
 * Example from
 *   http://www.sable.mcgill.ca/pipermail/soot-list/2006-April/000659.html
 */
public class ContainerTest2
{
  public static void main(String[] ps)
  {
    Container c1 = new Container();
    Item i1 = new Item();
    c1.setItem(i1);
           
    Container c2 = new Container();
    Item i2 = new Item();
    c2.setItem(i2);   
           
    Container c3 = c2;
  }
}

class Container
{
  private Item item = new Item();

  void setItem(Item item)
  {
    this.item = item;
  }

  Item getItem()
  {
    return this.item;
  }
}

class Item
{
}
