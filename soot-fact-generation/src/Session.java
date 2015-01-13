import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import soot.Unit;

public class Session
{
  private Map<String, Integer> _map = new HashMap<String, Integer>();

  public int nextNumber(String s)
  {
    Integer x = _map.get(s);

    if(x == null)
    {
      x = new Integer(0);
    }

    _map.put(s, new Integer(x.intValue() + 1));

    return x.intValue();
  }

  private Map<Unit, Integer> _units = new HashMap<Unit, Integer>();

  public void numberUnits(Iterator<Unit> iterator)
  {
    int index = 0;

    while(iterator.hasNext())
    {
      _units.put(iterator.next(), new Integer(index));
      index++;
    }
  }

  public int getUnitNumber(Unit u)
  {
    return _units.get(u);
  }

  private Unit _currentUnit;

  public void setCurrentUnit(Unit u)
  {
    _currentUnit = u;
  }

  public Unit getCurrentUnit()
  {
    return _currentUnit;
  }
}
