package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ Future, ExecutionContext }

/**
 * A repository for people.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class PersonRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have a name of people
   */
  private class PeopleTable(tag: Tag) extends Table[Person](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def age = column[Int]("age")

    /**
     * This is the tables default "projection".
     *
     * It defines how the columns are converted to and from the Person object.
     *
     * In this case, we are simply passing the id, name and page parameters to the Person case classes
     * apply and unapply methods.
     */
    def * = (id, name, age) <> ((Person.apply _).tupled, Person.unapply)
  }

  /**
   * The starting point for all queries on the people table.
   */
  private val people = TableQuery[PeopleTable]

  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(name: String, age: Int): Future[Person] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.age))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameAge, id) => Person(id, nameAge._1, nameAge._2))
    // And finally, insert the person into the database
    ) += (name, age)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Person]] = db.run {
    people.result
  }


  //This deletes a particular item in a database
  def del(id: Long) : Future[Int] = db.run {
    people.filter(_.id === id).delete
  }

  /** We shall be creating a query to update a user in the database
    * the Equivalent statement in SQL is "update PERSON set NAME='Some name' where ID='Some id".
    * To do that we will be creating a function called updateUser, which accepts 2 parameters: the
    * 'id' and 'name'. We will be updating the name where the Id = some id
    *
    * We can interprete the statement below as where the _.id == id, change p.name to another
    * name
    */
  //This method updates a table
  def update (id: Long, name: String) : Future[Int] = db.run {
    people.filter(_.id === id).map(p => (p.name)).update((name))
  }

  /***
    * We will now attempt a query to retrieve a particular user in the database
    * In SQl we could put it like this:
    * SELECT * FROM 'sometable' WHERE name = 'somename'. In slick however, a
    * different syntax is used to achieve the same result
    * So let's define a function and call it select which accepts just a single
    * parameter which is the name of the user we want to retrieve.
    * The we will use the 'filter()' function to select the user and 'result' function to
    * display the user
    */

  //this selects a particular user
  def select (name: String): Future[Seq[Person]] = db.run {
    /**the equivalent sql statement is select * from table
      where name == somename
      **/
    people.filter(p => p.name === name).result
  }

  /**what we are just saying above is is select * from table
      where name == somename
    **/

  def insertSample (): Future[Int] = db.run {
    people.map(p => (p.name, p.age)) += ("John", 25)
  }
}
