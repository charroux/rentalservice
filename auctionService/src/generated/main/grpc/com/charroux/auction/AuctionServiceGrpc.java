package com.charroux.auction;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: auctionService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuctionServiceGrpc {

  private AuctionServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.charroux.AuctionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.charroux.auction.CreditApplication,
      com.charroux.auction.Auction> getRentCarsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RentCars",
      requestType = com.charroux.auction.CreditApplication.class,
      responseType = com.charroux.auction.Auction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.charroux.auction.CreditApplication,
      com.charroux.auction.Auction> getRentCarsMethod() {
    io.grpc.MethodDescriptor<com.charroux.auction.CreditApplication, com.charroux.auction.Auction> getRentCarsMethod;
    if ((getRentCarsMethod = AuctionServiceGrpc.getRentCarsMethod) == null) {
      synchronized (AuctionServiceGrpc.class) {
        if ((getRentCarsMethod = AuctionServiceGrpc.getRentCarsMethod) == null) {
          AuctionServiceGrpc.getRentCarsMethod = getRentCarsMethod =
              io.grpc.MethodDescriptor.<com.charroux.auction.CreditApplication, com.charroux.auction.Auction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RentCars"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.CreditApplication.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.Auction.getDefaultInstance()))
              .setSchemaDescriptor(new AuctionServiceMethodDescriptorSupplier("RentCars"))
              .build();
        }
      }
    }
    return getRentCarsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.charroux.auction.Bidding,
      com.charroux.auction.BidResponse> getCarAuctionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CarAuction",
      requestType = com.charroux.auction.Bidding.class,
      responseType = com.charroux.auction.BidResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.charroux.auction.Bidding,
      com.charroux.auction.BidResponse> getCarAuctionMethod() {
    io.grpc.MethodDescriptor<com.charroux.auction.Bidding, com.charroux.auction.BidResponse> getCarAuctionMethod;
    if ((getCarAuctionMethod = AuctionServiceGrpc.getCarAuctionMethod) == null) {
      synchronized (AuctionServiceGrpc.class) {
        if ((getCarAuctionMethod = AuctionServiceGrpc.getCarAuctionMethod) == null) {
          AuctionServiceGrpc.getCarAuctionMethod = getCarAuctionMethod =
              io.grpc.MethodDescriptor.<com.charroux.auction.Bidding, com.charroux.auction.BidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CarAuction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.Bidding.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.BidResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuctionServiceMethodDescriptorSupplier("CarAuction"))
              .build();
        }
      }
    }
    return getCarAuctionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.charroux.auction.CarToBeRented> getCarsToBeRentedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CarsToBeRented",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.charroux.auction.CarToBeRented.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.charroux.auction.CarToBeRented> getCarsToBeRentedMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.charroux.auction.CarToBeRented> getCarsToBeRentedMethod;
    if ((getCarsToBeRentedMethod = AuctionServiceGrpc.getCarsToBeRentedMethod) == null) {
      synchronized (AuctionServiceGrpc.class) {
        if ((getCarsToBeRentedMethod = AuctionServiceGrpc.getCarsToBeRentedMethod) == null) {
          AuctionServiceGrpc.getCarsToBeRentedMethod = getCarsToBeRentedMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.charroux.auction.CarToBeRented>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CarsToBeRented"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.CarToBeRented.getDefaultInstance()))
              .setSchemaDescriptor(new AuctionServiceMethodDescriptorSupplier("CarsToBeRented"))
              .build();
        }
      }
    }
    return getCarsToBeRentedMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuctionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceStub>() {
        @java.lang.Override
        public AuctionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceStub(channel, callOptions);
        }
      };
    return AuctionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuctionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceBlockingStub>() {
        @java.lang.Override
        public AuctionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceBlockingStub(channel, callOptions);
        }
      };
    return AuctionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuctionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceFutureStub>() {
        @java.lang.Override
        public AuctionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceFutureStub(channel, callOptions);
        }
      };
    return AuctionServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<com.charroux.auction.CreditApplication> rentCars(
        io.grpc.stub.StreamObserver<com.charroux.auction.Auction> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getRentCarsMethod(), responseObserver);
    }

    /**
     */
    default io.grpc.stub.StreamObserver<com.charroux.auction.Bidding> carAuction(
        io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getCarAuctionMethod(), responseObserver);
    }

    /**
     */
    default void carsToBeRented(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.charroux.auction.CarToBeRented> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCarsToBeRentedMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuctionService.
   */
  public static abstract class AuctionServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuctionServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuctionService.
   */
  public static final class AuctionServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AuctionServiceStub> {
    private AuctionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.charroux.auction.CreditApplication> rentCars(
        io.grpc.stub.StreamObserver<com.charroux.auction.Auction> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRentCarsMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.charroux.auction.Bidding> carAuction(
        io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getCarAuctionMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void carsToBeRented(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.charroux.auction.CarToBeRented> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCarsToBeRentedMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuctionService.
   */
  public static final class AuctionServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuctionServiceBlockingStub> {
    private AuctionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.charroux.auction.CarToBeRented carsToBeRented(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCarsToBeRentedMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuctionService.
   */
  public static final class AuctionServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuctionServiceFutureStub> {
    private AuctionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.charroux.auction.CarToBeRented> carsToBeRented(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCarsToBeRentedMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CARS_TO_BE_RENTED = 0;
  private static final int METHODID_RENT_CARS = 1;
  private static final int METHODID_CAR_AUCTION = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CARS_TO_BE_RENTED:
          serviceImpl.carsToBeRented((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.charroux.auction.CarToBeRented>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_RENT_CARS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.rentCars(
              (io.grpc.stub.StreamObserver<com.charroux.auction.Auction>) responseObserver);
        case METHODID_CAR_AUCTION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.carAuction(
              (io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getRentCarsMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.charroux.auction.CreditApplication,
              com.charroux.auction.Auction>(
                service, METHODID_RENT_CARS)))
        .addMethod(
          getCarAuctionMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.charroux.auction.Bidding,
              com.charroux.auction.BidResponse>(
                service, METHODID_CAR_AUCTION)))
        .addMethod(
          getCarsToBeRentedMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.charroux.auction.CarToBeRented>(
                service, METHODID_CARS_TO_BE_RENTED)))
        .build();
  }

  private static abstract class AuctionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuctionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.charroux.auction.AuctionServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuctionService");
    }
  }

  private static final class AuctionServiceFileDescriptorSupplier
      extends AuctionServiceBaseDescriptorSupplier {
    AuctionServiceFileDescriptorSupplier() {}
  }

  private static final class AuctionServiceMethodDescriptorSupplier
      extends AuctionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuctionServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AuctionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuctionServiceFileDescriptorSupplier())
              .addMethod(getRentCarsMethod())
              .addMethod(getCarAuctionMethod())
              .addMethod(getCarsToBeRentedMethod())
              .build();
        }
      }
    }
    return result;
  }
}
