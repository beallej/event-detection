class AbstractClusterer:
  """
  Abstact Validator: An interface for validators, which validate queries with articles
  """
  def __init__(self):
      """
      obligatory init
      :return: nothing
      """
      pass

  def cluster(self, matrix, cutoff, article_titles, article_ids):
      assert False