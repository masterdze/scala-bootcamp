package com.evolutiongaming.bootcamp.error_handling

import cats.data.NonEmptyList
import com.evolutiongaming.bootcamp.error_handling.Validated.Faculty.{Biology, History, IT}
import com.evolutiongaming.bootcamp.error_handling.Validated.Student.{ValidatedNel, ValidationError}
import com.evolutiongaming.bootcamp.typeclass.Functor

import scala.concurrent.{ExecutionContext, Future}

object Validated extends App {

  sealed trait Faculty
  object Faculty {
    case object IT      extends Faculty
    case object History extends Faculty
    case object Biology extends Faculty
  }

  sealed abstract case class Student private (firstName: String, lastName: String, age: Int, faculty: Faculty)

  object Student {

    sealed abstract class ValidationError(message: String) extends Throwable(message) {
      override def toString: String = message
    }

    sealed abstract class NameError(message: String) extends ValidationError(message)
    object NameError {
      case object IllegalCharacter extends NameError("Name must contain only letters")
      case object EmptyName        extends NameError("Name must not be empty")
      case object TooLong          extends NameError("Name is too long")
    }

    sealed abstract class AgeError(message: String) extends ValidationError(message)
    object AgeError {
      case object NegativeAge extends AgeError("Age must be positive")
      case object TooBig      extends AgeError("Age is too big")
    }

    case object InvalidFaculty extends ValidationError("Invalid faculty")



    private def validateNameEither(string: String): Either[NameError, String] = {
      if (string.isEmpty) Left(NameError.EmptyName)
      else if (string.length > 20) Left(NameError.TooLong)
      else if (!string.matches("[a-zA-Z]*")) Left(NameError.IllegalCharacter)
      else Right(string)
    }

    // Exercise: implement these three methods
    private def validateAgeEither(n: Int): Either[AgeError, Int] = ???

    private def validateFacultyEither(string: String): Either[ValidationError, Faculty] = ???

    def validateEither(
      firstName: String,
      lastName:  String,
      age:       Int,
      faculty:   String
    ): Either[ValidationError, Student] = ???



    // Question: who knows my real name?
    trait Failable[F[_], E] extends Functor[F] {
      def pure[A](value: A): F[A]
      def raise[A](error: E): F[A]
      def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
    }

    object Failable {

      // Summoner
      def apply[F[_], E](implicit instance: Failable[F, E]): Failable[F, E] = instance

      implicit def optionFailable[E]: Failable[Option, E] = new Failable[Option, E] {
        override def pure[A](value: A): Option[A] = Some(value)
        override def raise[A](error: E): Option[A] = None // Option[Nothing]
        override def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
        override def product[A, B](fa: Option[A], fb: Option[B]): Option[(A, B)] = for {
          a <- fa
          b <- fb
        } yield (a, b)
      }

      implicit def raiseFuture[E <: Throwable](implicit ec: ExecutionContext): Failable[Future, E] = new Failable[Future, E] {
        override def pure[A](value: A): Future[A] = Future.successful(value)
        override def raise[A](error: E): Future[A] = Future.failed(error)
        override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
        override def product[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] = for {
          a <- fa
          b <- fb
        } yield (a, b)
      }

      // Exercise: implement
      implicit def eitherFailable[E]: Failable[Either[E, *], E] = ???
    }

    implicit final class FailableAnySyntax[T](val value: T) extends AnyVal {
      def pure[F[_], A >: T](implicit F: Failable[F, _]): F[A] = F.pure(value)
      def raise[F[_], A](implicit F: Failable[F, _ >: T]): F[A] = F.raise(value)
    }

    implicit final class FailableSyntax[F[_], A](val value: F[A]) extends AnyVal {
      def map[B](f: A => B)(implicit F: Failable[F, _]): F[B] = F.map(value)(f)
      def product[B](fb: F[B])(implicit F: Failable[F, _]): F[(A, B)] = F.product(value, fb)
    }



    private def validateName[F[_]: Failable[*[_], ValidationError]](string: String): F[String] = {
      if (string.isEmpty) NameError.EmptyName.raise
      else if (string.length > 20) NameError.TooLong.raise
      else if (!string.matches("[a-zA-Z]*")) NameError.IllegalCharacter.raise
      else string.pure
    }

    // Exercise: implement the following 3 methods
    private def validateAge[F[_]: Failable[*[_], ValidationError]](n: Int): F[Int] = ???

    private def validateFaculty[F[_]: Failable[*[_], ValidationError]](string: String): F[Faculty] = ???

    def validateFailable[F[_]: Failable[*[_], ValidationError]](
      firstName: String,
      lastName:  String,
      age:       Int,
      faculty:   String
    ): F[Student] = ???



    sealed trait ValidatedNel[E, A]

    object ValidatedNel {

      final case class Invalid[E, A](errors: NonEmptyList[E]) extends ValidatedNel[E, A]
      final case class Valid[E, A](value: A) extends ValidatedNel[E, A]

      // Exercise: implement
      implicit def failable[E]: Failable[ValidatedNel[E, *], E] = ???
    }
  }

//  println(Student.validateEither("123", "456", -1, "Art"))
//
//
//
//  println(Student.validateFailable[Option]("123", "456", -1, "Art"))
//  println(Student.validateFailable[Option]("John", "Dow", 18, "IT"))
//
//  println(Student.validateFailable[Either[ValidationError, *]]("123", "456", -1, "Art"))
//  println(Student.validateFailable[Either[ValidationError, *]]("John", "Dow", 18, "IT"))
//
//
//  println(Student.validateFailable[ValidatedNel[ValidationError, *]]("123", "456", -1, "Art"))
//  println(Student.validateFailable[ValidatedNel[ValidationError, *]]("John", "Dow", 18, "IT"))
}
